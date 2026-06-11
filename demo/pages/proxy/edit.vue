<template>
	<view class="page">
		<view class="form-section">
			<view class="form-row">
				<text class="form-label">方案名称</text>
				<input
					class="form-input"
					v-model="form.name"
					placeholder="如：工作代理"
					placeholder-class="form-placeholder"
				/>
			</view>
			<view class="form-row">
				<text class="form-label">服务器地址</text>
				<input
					class="form-input"
					v-model="form.host"
					placeholder="如：127.0.0.1"
					placeholder-class="form-placeholder"
				/>
			</view>
			<view class="form-row last">
				<text class="form-label">端口</text>
				<input
					class="form-input"
					v-model="form.port"
					type="number"
					placeholder="如：7890"
					placeholder-class="form-placeholder"
				/>
			</view>
		</view>

		<view class="btn-area">
			<view class="btn-save" @tap="save">
				<text class="btn-save-text">保存</text>
			</view>
		</view>
	</view>
</template>

<script>
	export default {
		data() {
			return {
				editId: '',
				form: {
					name: '',
					host: '',
					port: '',
				},
				eventChannel: null,
			};
		},
		onLoad(options) {
			this.eventChannel = this.getOpenerEventChannel();
			if (options.id) {
				this.editId = options.id;
				this.loadScheme(options.id);
			}
		},
		methods: {
			loadScheme(id) {
				try {
					const data = uni.getStorageSync('proxy_config');
					if (data) {
						const parsed = JSON.parse(data);
						const scheme = (parsed.schemes || []).find(s => s.id === id);
						if (scheme) {
							this.form.name = scheme.name;
							this.form.host = scheme.host;
							this.form.port = String(scheme.port);
						}
					}
				} catch (e) {}
			},
			save() {
				const name = (this.form.name || '').trim();
				const host = (this.form.host || '').trim();
				const port = parseInt(this.form.port);

				if (!name) {
					uni.showToast({ title: '请输入方案名称', icon: 'none' });
					return;
				}
				if (!host) {
					uni.showToast({ title: '请输入服务器地址', icon: 'none' });
					return;
				}
				if (!port || port <= 0 || port > 65535) {
					uni.showToast({ title: '请输入有效端口(1-65535)', icon: 'none' });
					return;
				}

				const scheme = {
					id: this.editId || 'proxy_' + Date.now(),
					name,
					host,
					port,
				};

				if (this.eventChannel) {
					this.eventChannel.emit('saved', scheme);
				}
				uni.navigateBack();
			},
		},
	};
</script>

<style scoped>
	.page {
		min-height: 100vh;
		background-color: #0C0A09;
		padding-bottom: env(safe-area-inset-bottom);
	}

	.form-section {
		margin: 24rpx 32rpx;
		background-color: #1C1917;
		border-radius: 20rpx;
		overflow: hidden;
		border: 1rpx solid #44403C;
	}
	.form-row {
		display: flex;
		align-items: center;
		padding: 28rpx 32rpx;
		border-bottom: 1rpx solid #44403C;
	}
	.form-row.last {
		border-bottom: none;
	}
	.form-label {
		font-size: 30rpx;
		color: #FAFAF9;
		width: 180rpx;
		flex-shrink: 0;
	}
	.form-input {
		flex: 1;
		font-size: 30rpx;
		color: #FAFAF9;
		text-align: right;
	}
	.form-placeholder {
		color: #57534E;
		font-size: 28rpx;
	}

	.btn-area {
		padding: 40rpx 32rpx;
	}
	.btn-save {
		background-color: #EAB308;
		border-radius: 20rpx;
		padding: 24rpx 0;
		text-align: center;
	}
	.btn-save-text {
		font-size: 34rpx;
		font-weight: 600;
		color: #0C0A09;
	}
</style>
