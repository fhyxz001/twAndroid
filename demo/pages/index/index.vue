<template>
	<view class="content">
		<image class="logo" src="/static/logo.png"></image>
		<view class="text-area">
			<text class="title">TW Downloader</text>
		</view>
		<view class="entry-list">
			<view class="entry-card" @tap="goWaterfall">
				<text class="entry-icon">&#9634;</text>
				<view class="entry-info">
					<text class="entry-title">瀑布流预览</text>
					<text class="entry-desc">浏览海报，选择下载</text>
				</view>
				<text class="entry-arrow">&rarr;</text>
			</view>
		</view>
	</view>
</template>

<script>
	export default {
		data() {
			return {
				title: 'Hello'
			}
		},
		onLoad() {

		},
		methods: {
			goWaterfall() {
				uni.navigateTo({
					url: '/pages/waterfall/waterfall'
				});
			}
		}
	}
</script>

<style>
	.content {
		display: flex;
		flex-direction: column;
		align-items: center;
		padding: 40rpx;
		background-color: #0C0A09;
		min-height: 100vh;
	}

	.logo {
		height: 200rpx;
		width: 200rpx;
		margin-top: 120rpx;
		margin-bottom: 40rpx;
	}

	.text-area {
		display: flex;
		justify-content: center;
		margin-bottom: 60rpx;
	}

	.title {
		font-size: 40rpx;
		font-weight: 800;
		color: #EAB308;
		letter-spacing: 2rpx;
	}

	.entry-list {
		width: 100%;
	}

	.entry-card {
		display: flex;
		align-items: center;
		padding: 32rpx 28rpx;
		background-color: #1C1917;
		border: 2rpx solid #44403C;
		border-radius: 16rpx;
		box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.4);
	}

	.entry-icon {
		font-size: 48rpx;
		color: #EAB308;
		margin-right: 24rpx;
	}

	.entry-info {
		flex: 1;
		display: flex;
		flex-direction: column;
		gap: 6rpx;
	}

	.entry-title {
		font-size: 30rpx;
		font-weight: 700;
		color: #FAFAF9;
	}

	.entry-desc {
		font-size: 24rpx;
		color: #A8A29E;
	}

	.entry-arrow {
		font-size: 32rpx;
		color: #EAB308;
		font-weight: 700;
	}
</style>
