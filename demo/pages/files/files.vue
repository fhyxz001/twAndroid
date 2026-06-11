<template>
	<view class="page">
		<!-- 导航栏 -->
		<view class="nav-bar">
			<view class="nav-content">
				<view class="nav-header-row">
					<view class="nav-back" @tap="goBack">
						<text class="nav-back-icon">&#8249;</text>
						<text class="nav-back-text">返回</text>
					</view>
					<text class="nav-title">本地文件</text>
					<view class="nav-action" @tap="toggleEditMode">
						<text class="nav-action-text">{{ isEditMode ? '完成' : '编辑' }}</text>
					</view>
				</view>
			</view>
		</view>

		<!-- 文件列表 -->
		<scroll-view class="file-scroll" scroll-y>
			<view class="file-list" v-if="files.length > 0">
				<view
					v-for="(file, index) in files"
					:key="file.id + '-' + index"
					class="swipe-wrapper"
				>
					<!-- 滑动内容区 -->
					<view
						class="swipe-content"
						:style="{ transform: 'translateX(' + (swipeOffset[index] || 0) + 'rpx)' }"
						@touchstart="onTouchStart($event, index)"
						@touchmove="onTouchMove($event, index)"
						@touchend="onTouchEnd($event, index)"
					>
						<view
							:class="['file-item', isEditMode && selectedIds.has(index) ? 'file-item-selected' : '']"
							@tap="onItemTap(index)"
							@longpress="onItemLongPress(index)"
						>
							<!-- 选择框 -->
							<view class="file-check" v-if="isEditMode" @tap.stop="toggleSelect(index)">
								<view :class="['check-circle', selectedIds.has(index) ? 'check-circle-on' : '']">
									<text v-if="selectedIds.has(index)" class="check-mark">&#10003;</text>
								</view>
							</view>
							<!-- 缩略图 -->
							<view class="file-thumb">
								<image v-if="file.thumbnail" class="thumb-img" :src="file.thumbnail" mode="aspectFill" />
								<view v-else class="thumb-fallback">
									<text class="thumb-fallback-icon">&#9654;</text>
								</view>
								<view class="thumb-play">
									<text class="thumb-play-icon">&#9654;</text>
								</view>
							</view>
							<!-- 信息 -->
							<view class="file-info">
								<text class="file-name">{{ file.title }}</text>
								<text class="file-date">{{ formatDate(file.downloadedAt) }}</text>
							</view>
						</view>
					</view>
					<!-- 左滑删除按钮 -->
					<view class="swipe-actions" v-if="!isEditMode">
						<view class="swipe-btn swipe-btn-delete" @tap="deleteOne(index)">
							<text class="swipe-btn-text">删除</text>
						</view>
					</view>
				</view>
			</view>

			<!-- 空状态 -->
			<view class="empty" v-else>
				<text class="empty-icon">&#128193;</text>
				<text class="empty-title">暂无下载文件</text>
				<text class="empty-desc">去探索页面下载视频吧</text>
			</view>

			<!-- 底部留白 -->
			<view class="bottom-spacer"></view>
		</scroll-view>

		<!-- 编辑模式底部栏 -->
		<view class="edit-bar" v-if="isEditMode">
			<view class="edit-bar-inner">
				<text class="edit-select-all" @tap="toggleSelectAll">
					{{ isAllSelected ? '取消全选' : '全选' }}
				</text>
				<text class="edit-count">已选 {{ selectedIds.size }} 项</text>
				<text
					:class="['edit-delete', selectedIds.size === 0 ? 'edit-delete-disabled' : '']"
					@tap="deleteSelected"
				>
					删除
				</text>
			</view>
		</view>
	</view>
</template>

<script>

	const DELETE_BTN_WIDTH = 160; // rpx

	export default {
		data() {
			return {
				files: [],
				isEditMode: false,
				selectedIds: new Set(),
				swipeOffset: {},
				touchStartX: 0,
				touchStartY: 0,
				touchCurrentX: 0,
				swiping: false,
				swipingIndex: -1,
				openIndex: -1,
			};
		},
		computed: {
			isAllSelected() {
				return this.files.length > 0 && this.files.every((_, i) => this.selectedIds.has(i));
			},
			fileVideoList() {
				return this.files.map(f => ({
					src: f.filePath || f.url,
					description: f.title || '',
					poster: f.thumbnail || '',
					objectFit: 'contain',
					userAvatar: null,
					userName: null,
					userFollow: null,
				}));
			},
		},
		onShow() {
			this.loadFiles();
		},
		methods: {
			loadFiles() {
				try {
					this.files = JSON.parse(uni.getStorageSync('download_records') || '[]');
				} catch (e) {
					this.files = [];
				}
			},
			formatDate(timestamp) {
				if (!timestamp) return '';
				const d = new Date(timestamp);
				const y = d.getFullYear();
				const m = String(d.getMonth() + 1).padStart(2, '0');
				const day = String(d.getDate()).padStart(2, '0');
				const h = String(d.getHours()).padStart(2, '0');
				const min = String(d.getMinutes()).padStart(2, '0');
				return `${y}/${m}/${day} ${h}:${min}`;
			},
			goBack() {
				uni.navigateBack({ delta: 1 });
			},
			toggleEditMode() {
				this.isEditMode = !this.isEditMode;
				if (!this.isEditMode) this.selectedIds = new Set();
				this.closeAllSwipe();
			},
			toggleSelect(index) {
				const newSet = new Set(this.selectedIds);
				if (newSet.has(index)) newSet.delete(index);
				else newSet.add(index);
				this.selectedIds = newSet;
			},
			toggleSelectAll() {
				const newSet = new Set(this.selectedIds);
				if (this.isAllSelected) {
					this.files.forEach((_, i) => newSet.delete(i));
				} else {
					this.files.forEach((_, i) => newSet.add(i));
				}
				this.selectedIds = newSet;
			},
			onItemTap(index) {
				if (this.isEditMode) {
					this.toggleSelect(index);
				} else {
					if (this.openIndex === index) {
						this.closeSwipe(index);
						return;
					}
					if (this.openIndex >= 0) {
						this.closeSwipe(this.openIndex);
						return;
					}
					this.playFile(index);
				}
			},
			onItemLongPress(index) {
				if (!this.isEditMode) {
					this.isEditMode = true;
					this.selectedIds = new Set([index]);
					this.closeAllSwipe();
				}
			},

			// ===== 左滑删除 =====
			onTouchStart(e, index) {
				if (this.isEditMode) return;
				const touch = e.touches[0];
				this.touchStartX = touch.clientX;
				this.touchStartY = touch.clientY;
				this.touchCurrentX = touch.clientX;
				this.swiping = false;
				this.swipingIndex = index;
				if (this.openIndex >= 0 && this.openIndex !== index) {
					this.$set(this.swipeOffset, this.openIndex, 0);
					this.openIndex = -1;
				}
			},
			onTouchMove(e, index) {
				if (this.isEditMode || this.swipingIndex !== index) return;
				const touch = e.touches[0];
				const deltaX = touch.clientX - this.touchStartX;
				const deltaY = touch.clientY - this.touchStartY;

				if (!this.swiping) {
					if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > 10) {
						this.swiping = true;
					} else {
						return;
					}
				}

				this.touchCurrentX = touch.clientX;
				const pxToRpx = 750 / uni.getSystemInfoSync().windowWidth;
				const currentOffset = this.swipeOffset[index] || 0;
				let newOffset = currentOffset + deltaX * pxToRpx;

				if (newOffset > 0) newOffset = 0;
				if (newOffset < -DELETE_BTN_WIDTH * 1.2) newOffset = -DELETE_BTN_WIDTH * 1.2;

				this.$set(this.swipeOffset, index, newOffset);
			},
			onTouchEnd(e, index) {
				if (this.isEditMode || this.swipingIndex !== index) return;
				if (!this.swiping) {
					this.swipingIndex = -1;
					return;
				}
				this.swiping = false;
				this.swipingIndex = -1;

				const currentOffset = this.swipeOffset[index] || 0;
				if (currentOffset < -DELETE_BTN_WIDTH / 2) {
					this.$set(this.swipeOffset, index, -DELETE_BTN_WIDTH);
					this.openIndex = index;
				} else {
					this.$set(this.swipeOffset, index, 0);
					if (this.openIndex === index) this.openIndex = -1;
				}
			},
			closeSwipe(index) {
				this.$set(this.swipeOffset, index, 0);
				if (this.openIndex === index) this.openIndex = -1;
			},
			closeAllSwipe() {
				for (const key in this.swipeOffset) {
					if (this.swipeOffset[key] !== 0) {
						this.$set(this.swipeOffset, key, 0);
					}
				}
				this.openIndex = -1;
			},

			playFile(index) {
				if (!this.files.length) return;
				const list = this.fileVideoList;
				uni.navigateTo({
					url: '/pages/play/play',
					success: (res) => {
						res.eventChannel.emit('initData', { list, index });
					},
				});
			},
			deleteOne(index) {
				this.closeSwipe(index);
				uni.showModal({
					title: '确认删除',
					content: `删除「${this.files[index].title}」？`,
					success: (res) => {
						if (res.confirm) {
							this.removeFiles([index]);
						}
					},
				});
			},
			deleteSelected() {
				if (this.selectedIds.size === 0) return;
				uni.showModal({
					title: '确认删除',
					content: `确定删除选中的 ${this.selectedIds.size} 个文件？`,
					success: (res) => {
						if (res.confirm) {
							this.removeFiles([...this.selectedIds]);
							this.isEditMode = false;
							this.selectedIds = new Set();
						}
					},
				});
			},
			removeFiles(indices) {
				indices.sort((a, b) => b - a);
				const newFiles = [...this.files];
				for (const i of indices) {
					const file = newFiles[i];
					if (file && file.filePath) {
						uni.removeSavedFile({ filePath: file.filePath });
					}
					newFiles.splice(i, 1);
				}
				this.files = newFiles;
				this.swipeOffset = {};
				this.openIndex = -1;
				uni.setStorageSync('download_records', JSON.stringify(this.files));
			},
		},
	};
</script>

<style scoped>
	.page {
		display: flex;
		flex-direction: column;
		height: 100vh;
		background-color: #0C0A09;
	}

	/* 导航栏 */
	.nav-bar {
		background-color: #0C0A09;
		padding-top: var(--status-bar-height, 44px);
	}
	.nav-content {
		padding: 12rpx 32rpx 16rpx;
	}
	.nav-header-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}
	.nav-back {
		display: flex;
		align-items: center;
		gap: 4rpx;
	}
	.nav-back-icon {
		font-size: 44rpx;
		color: #EAB308;
		font-weight: 300;
	}
	.nav-back-text {
		font-size: 30rpx;
		color: #EAB308;
	}
	.nav-title {
		font-size: 34rpx;
		font-weight: 600;
		color: #FAFAF9;
	}
	.nav-action {
		padding: 8rpx 16rpx;
	}
	.nav-action-text {
		font-size: 30rpx;
		color: #EAB308;
		font-weight: 500;
	}

	/* 文件列表 */
	.file-scroll {
		flex: 1;
		overflow: hidden;
	}
	.file-list {
		padding: 0 32rpx;
	}

	/* ===== 左滑删除容器 ===== */
	.swipe-wrapper {
		position: relative;
		margin-bottom: 12rpx;
		overflow: hidden;
		border-radius: 20rpx;
	}
	.swipe-content {
		position: relative;
		z-index: 2;
		transition: transform 0.3s cubic-bezier(0.32, 0.72, 0, 1);
	}
	.swipe-actions {
		position: absolute;
		right: 0;
		top: 0;
		bottom: 0;
		z-index: 1;
		display: flex;
		align-items: stretch;
	}
	.swipe-btn {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 160rpx;
	}
	.swipe-btn-delete {
		background-color: #EF4444;
		border-radius: 0 20rpx 20rpx 0;
	}
	.swipe-btn-text {
		color: #fff;
		font-size: 28rpx;
		font-weight: 600;
	}

	/* 文件项 */
	.file-item {
		display: flex;
		align-items: center;
		padding: 20rpx 24rpx;
		background-color: #1C1917;
		gap: 20rpx;
	}
	.file-item-selected {
		background-color: rgba(234, 179, 8, 0.08);
	}

	/* 选择框 */
	.file-check {
		flex-shrink: 0;
	}
	.check-circle {
		width: 44rpx;
		height: 44rpx;
		border-radius: 50%;
		border: 3rpx solid #57534E;
		display: flex;
		align-items: center;
		justify-content: center;
		transition: all 0.15s ease;
	}
	.check-circle-on {
		background-color: #EAB308;
		border-color: #EAB308;
	}
	.check-mark {
		color: #0C0A09;
		font-size: 24rpx;
		font-weight: 700;
	}

	/* 缩略图 */
	.file-thumb {
		position: relative;
		width: 120rpx;
		height: 90rpx;
		border-radius: 12rpx;
		overflow: hidden;
		flex-shrink: 0;
		background-color: #292524;
	}
	.thumb-img {
		width: 100%;
		height: 100%;
	}
	.thumb-fallback {
		width: 100%;
		height: 100%;
		display: flex;
		align-items: center;
		justify-content: center;
	}
	.thumb-fallback-icon {
		color: rgba(255, 255, 255, 0.3);
		font-size: 32rpx;
	}
	.thumb-play {
		position: absolute;
		inset: 0;
		display: flex;
		align-items: center;
		justify-content: center;
	}
	.thumb-play-icon {
		color: #EAB308;
		font-size: 28rpx;
		text-shadow: 0 1rpx 4rpx rgba(0, 0, 0, 0.5);
	}

	/* 文件信息 */
	.file-info {
		flex: 1;
		min-width: 0;
	}
	.file-name {
		font-size: 28rpx;
		font-weight: 500;
		color: #FAFAF9;
		line-height: 1.4;
		display: -webkit-box;
		-webkit-box-orient: vertical;
		-webkit-line-clamp: 2;
		overflow: hidden;
	}
	.file-date {
		font-size: 24rpx;
		color: #A8A29E;
		margin-top: 6rpx;
		display: block;
	}

	/* 空状态 */
	.empty {
		padding: 160rpx 40rpx;
		text-align: center;
	}
	.empty-icon {
		font-size: 80rpx;
		display: block;
		margin-bottom: 20rpx;
	}
	.empty-title {
		font-size: 34rpx;
		font-weight: 600;
		color: #FAFAF9;
		display: block;
		margin-bottom: 8rpx;
	}
	.empty-desc {
		font-size: 28rpx;
		color: #A8A29E;
		display: block;
	}

	.bottom-spacer {
		height: 140rpx;
	}

	/* 编辑模式底部栏 */
	.edit-bar {
		position: fixed;
		left: 0;
		right: 0;
		bottom: 0;
		z-index: 50;
		background-color: rgba(28, 25, 23, 0.95);
		backdrop-filter: blur(20px);
		border-top: 1rpx solid #44403C;
	}
	.edit-bar-inner {
		display: flex;
		align-items: center;
		padding: 16rpx 32rpx;
		padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
		gap: 20rpx;
	}
	.edit-select-all {
		font-size: 28rpx;
		color: #EAB308;
		font-weight: 500;
	}
	.edit-count {
		flex: 1;
		font-size: 26rpx;
		color: #A8A29E;
	}
	.edit-delete {
		font-size: 28rpx;
		color: #FAFAF9;
		font-weight: 600;
		padding: 14rpx 36rpx;
		border-radius: 20rpx;
		background-color: #EF4444;
	}
	.edit-delete-disabled {
		opacity: 0.4;
	}

</style>
