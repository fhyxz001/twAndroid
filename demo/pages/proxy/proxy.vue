<template>
	<view class="page">
		<!-- 全局开关 -->
		<view class="section">
			<view class="toggle-row">
				<text class="toggle-label">全局代理</text>
				<switch :checked="proxyEnabled" @change="onToggleProxy" color="#EAB308" />
			</view>
			<text class="toggle-hint">开启后将使用选中的代理方案</text>
		</view>

		<!-- 方案列表 -->
		<view class="section">
			<view class="section-header">
				<text class="section-title">代理方案</text>
				<view class="add-btn" @tap="addScheme">
					<text class="add-btn-text">+ 添加</text>
				</view>
			</view>
			<view class="scheme-list" v-if="schemes.length > 0">
				<view
					v-for="(scheme, index) in schemes"
					:key="scheme.id"
					class="scheme-item"
					@tap="selectScheme(scheme.id)"
				>
					<view class="scheme-radio">
						<view :class="['radio-circle', selectedId === scheme.id ? 'radio-checked' : '']">
							<view v-if="selectedId === scheme.id" class="radio-dot"></view>
						</view>
					</view>
					<view class="scheme-info">
						<text class="scheme-name">{{ scheme.name }}</text>
						<text class="scheme-addr">{{ scheme.host }}:{{ scheme.port }}</text>
					</view>
					<view class="scheme-actions">
						<view class="action-btn" @tap.stop="editScheme(scheme)">
							<text class="action-text">编辑</text>
						</view>
						<view class="action-btn action-delete" @tap.stop="deleteScheme(scheme.id)">
							<text class="action-text action-text-delete">删除</text>
						</view>
					</view>
				</view>
			</view>
			<view class="empty" v-else>
				<text class="empty-text">暂无代理方案</text>
				<text class="empty-hint">点击右上角"添加"创建代理方案</text>
			</view>
		</view>
	</view>
</template>

<script>
	export default {
		data() {
			return {
				proxyEnabled: false,
				schemes: [],
				selectedId: '',
			};
		},
		onLoad() {
			this.loadData();
		},
		onBackPress() {
			// 返回前保存并应用
			this.saveAndApply();
		},
		methods: {
			loadData() {
				try {
					const data = uni.getStorageSync('proxy_config');
					if (data) {
						const parsed = JSON.parse(data);
						this.proxyEnabled = parsed.enabled || false;
						this.schemes = parsed.schemes || [];
						this.selectedId = parsed.selectedId || '';
					}
				} catch (e) {}
			},
			saveData() {
				try {
					uni.setStorageSync('proxy_config', JSON.stringify({
						enabled: this.proxyEnabled,
						schemes: this.schemes,
						selectedId: this.selectedId,
					}));
				} catch (e) {}
			},
			saveAndApply() {
				this.saveData();
				this.applyProxy();
			},
			onToggleProxy(e) {
				this.proxyEnabled = e.detail.value;
				if (this.proxyEnabled && !this.selectedId) {
					uni.showToast({ title: '请先选择一个代理方案', icon: 'none' });
				}
				this.saveAndApply();
			},
			selectScheme(id) {
				this.selectedId = id;
				this.saveAndApply();
			},
			addScheme() {
				uni.navigateTo({
					url: '/pages/proxy/edit',
					events: {
						saved: (scheme) => {
							this.schemes.push(scheme);
							if (this.schemes.length === 1) {
								this.selectedId = scheme.id;
							}
							this.saveAndApply();
						},
					},
				});
			},
			editScheme(scheme) {
				uni.navigateTo({
					url: '/pages/proxy/edit?id=' + scheme.id,
					events: {
						saved: (updated) => {
							const idx = this.schemes.findIndex(s => s.id === scheme.id);
							if (idx >= 0) {
								this.schemes[idx] = updated;
								this.schemes = [...this.schemes];
							}
							this.saveAndApply();
						},
					},
				});
			},
			deleteScheme(id) {
				uni.showModal({
					title: '确认删除',
					content: '确定要删除该代理方案吗？',
					success: (res) => {
						if (res.confirm) {
							this.schemes = this.schemes.filter(s => s.id !== id);
							if (this.selectedId === id) {
								this.selectedId = this.schemes.length > 0 ? this.schemes[0].id : '';
							}
							this.saveAndApply();
						}
					},
				});
			},
			applyProxy() {
				const shouldProxy = this.proxyEnabled && this.selectedId;
				const scheme = shouldProxy ? this.schemes.find(s => s.id === this.selectedId) : null;

				if (typeof plus === 'undefined') return;
				try {
					const System = plus.android.importClass('java.lang.System');
					if (scheme) {
						System.setProperty('http.proxyHost', scheme.host);
						System.setProperty('http.proxyPort', String(scheme.port));
						System.setProperty('https.proxyHost', scheme.host);
						System.setProperty('https.proxyPort', String(scheme.port));
					} else {
						System.clearProperty('http.proxyHost');
						System.clearProperty('http.proxyPort');
						System.clearProperty('https.proxyHost');
						System.clearProperty('https.proxyPort');
					}
				} catch (e) {
					console.error('applyProxy error', e);
				}
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

	.section {
		margin: 24rpx 32rpx;
		background-color: #1C1917;
		border-radius: 20rpx;
		overflow: hidden;
		border: 1rpx solid #44403C;
	}

	.toggle-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 28rpx 32rpx;
	}
	.toggle-label {
		font-size: 32rpx;
		font-weight: 600;
		color: #FAFAF9;
	}
	.toggle-hint {
		display: block;
		padding: 0 32rpx 24rpx;
		font-size: 24rpx;
		color: #A8A29E;
	}

	.section-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 28rpx 32rpx;
		border-bottom: 1rpx solid #44403C;
	}
	.section-title {
		font-size: 30rpx;
		font-weight: 600;
		color: #FAFAF9;
	}
	.add-btn {
		padding: 8rpx 24rpx;
		border-radius: 16rpx;
		background-color: rgba(234, 179, 8, 0.12);
	}
	.add-btn-text {
		font-size: 26rpx;
		color: #EAB308;
		font-weight: 500;
	}

	.scheme-list {
		padding: 0;
	}
	.scheme-item {
		display: flex;
		align-items: center;
		padding: 24rpx 32rpx;
		border-bottom: 1rpx solid #44403C;
	}
	.scheme-item:last-child {
		border-bottom: none;
	}

	.scheme-radio {
		margin-right: 20rpx;
	}
	.radio-circle {
		width: 44rpx;
		height: 44rpx;
		border-radius: 50%;
		border: 3rpx solid #57534E;
		display: flex;
		align-items: center;
		justify-content: center;
		transition: all 0.2s ease;
	}
	.radio-checked {
		border-color: #EAB308;
	}
	.radio-dot {
		width: 24rpx;
		height: 24rpx;
		border-radius: 50%;
		background-color: #EAB308;
	}

	.scheme-info {
		flex: 1;
	}
	.scheme-name {
		display: block;
		font-size: 30rpx;
		font-weight: 500;
		color: #FAFAF9;
	}
	.scheme-addr {
		display: block;
		font-size: 24rpx;
		color: #A8A29E;
		margin-top: 4rpx;
	}

	.scheme-actions {
		display: flex;
		gap: 16rpx;
	}
	.action-btn {
		padding: 8rpx 20rpx;
		border-radius: 12rpx;
		background-color: rgba(234, 179, 8, 0.1);
	}
	.action-text {
		font-size: 24rpx;
		color: #EAB308;
	}
	.action-delete {
		background-color: rgba(239, 68, 68, 0.1);
	}
	.action-text-delete {
		color: #EF4444;
	}

	.empty {
		padding: 60rpx 32rpx;
		text-align: center;
	}
	.empty-text {
		display: block;
		font-size: 30rpx;
		color: #A8A29E;
		margin-bottom: 8rpx;
	}
	.empty-hint {
		display: block;
		font-size: 24rpx;
		color: #57534E;
	}
</style>
