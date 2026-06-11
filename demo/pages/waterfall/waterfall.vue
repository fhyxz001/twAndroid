<template>
	<view class="page">
		<!-- 大标题导航栏 -->
		<view class="nav-bar">
			<view class="nav-content">
				<view class="nav-header-row">
					<text class="nav-large-title">探索</text>
					<view class="nav-actions">
						<view class="nav-btn" @tap="toggleSelectMode">
							<image class="nav-btn-icon" :src="selectMode ? '/static/select_blue.png' : '/static/select.png'" mode="aspectFit" />
						</view>
						<view class="nav-btn" @tap="goToProxy">
							<image class="nav-btn-icon" src="/static/clash.png" mode="aspectFit" />
						</view>
						<view class="nav-btn" @tap="goToFiles">
							<image class="nav-btn-icon" src="/static/list.png" mode="aspectFit" />
						</view>
						<view class="nav-btn" @tap="showSettings = true">
							<image class="nav-btn-icon" src="/static/set.png" mode="aspectFit" />
						</view>
						<view class="nav-btn" @tap="loadData">
							<image class="nav-btn-icon" src="/static/refresh.png" mode="aspectFit" />
						</view>
					</view>
				</view>

			</view>
		</view>

		<!-- 分类标签栏 -->
		<scroll-view class="tab-scroll" scroll-x show-scrollbar="false" enable-flex>
			<view class="tab-track">
				<view
					v-for="tab in tabs"
					:key="tab.code"
					:class="['tab-btn', currentTag === tab.code ? 'tab-btn-active' : '']"
					@tap="switchTab(tab.code)"
				>
					<text class="tab-btn-text">{{ tab.name }}</text>
				</view>
			</view>
		</scroll-view>

		<!-- 瀑布流 -->
		<scroll-view
			class="waterfall-scroll"
			scroll-y
			@scrolltolower="loadMore"
			:refresher-enabled="true"
			:refresher-triggered="refreshing"
			@refresherrefresh="onRefresh"
		>
			<!-- 状态提示 -->
			<view class="status-hint" v-if="loading">
				<view class="spinner"></view>
				<text class="status-hint-text">加载中...</text>
			</view>
			<view class="status-hint error" v-else-if="loadError">
				<text class="status-hint-text">{{ loadError }}</text>
			</view>

			<view class="waterfall-container">
				<!-- 左列 -->
				<view class="waterfall-col">
					<view
						v-for="item in leftItems"
						:key="item.id"
						:class="['card', selectMode && selectedIds.has(item.id) ? 'card-selected' : '']"
						@tap="onCardTap(item)"
					>
						<view class="poster">
							<image
								v-if="item.thumbnail"
								class="poster-img"
								:src="item.thumbnail"
								mode="widthFix"
								lazy-load
							/>
							<view v-else class="poster-fallback">
								<text class="fallback-icon">&#9654;</text>
							</view>
							<!-- 已下载标志 -->
							<image
								class="downloaded-badge"
								src="/static/dowloaded.png"
								mode="aspectFit"
								v-if="downloadedIds.has(item.id)"
							/>
							<!-- 选中指示器 -->
							<view class="select-indicator" v-if="selectMode" @tap.stop="toggleSelect(item.id)">
								<view :class="['indicator-circle', selectedIds.has(item.id) ? 'indicator-checked' : '']">
									<text v-if="selectedIds.has(item.id)" class="indicator-check">&#10003;</text>
								</view>
							</view>
							<!-- 播放按钮 -->
							<view class="play-badge" v-if="item.url">
								<text class="play-badge-icon">&#9654;</text>
							</view>
							<!-- 时长标签 -->
							<view class="duration-badge" v-if="item.duration">
								<text class="duration-text">{{ formatDuration(item.duration) }}</text>
							</view>
						</view>
						<view class="card-body">
							<!-- <text class="card-title">{{ item.title || item.id }}</text> -->
							<view class="card-stats">
								<text class="stat-item" v-if="item.pv">热度 {{ formatCount(item.pv) }}</text>
								<text class="stat-item" v-if="item.favorite">&#9829; {{ formatCount(item.favorite) }}</text>
							</view>
						</view>
					</view>
				</view>
				<!-- 右列 -->
				<view class="waterfall-col">
					<view
						v-for="item in rightItems"
						:key="item.id"
						:class="['card', selectMode && selectedIds.has(item.id) ? 'card-selected' : '']"
						@tap="onCardTap(item)"
					>
						<view class="poster">
							<image
								v-if="item.thumbnail"
								class="poster-img"
								:src="item.thumbnail"
								mode="widthFix"
								lazy-load
							/>
							<view v-else class="poster-fallback">
								<text class="fallback-icon">&#9654;</text>
							</view>
							<!-- 已下载标志 -->
							<image
								class="downloaded-badge"
								src="/static/dowloaded.png"
								mode="aspectFit"
								v-if="downloadedIds.has(item.id)"
							/>
							<view class="select-indicator" v-if="selectMode" @tap.stop="toggleSelect(item.id)">
								<view :class="['indicator-circle', selectedIds.has(item.id) ? 'indicator-checked' : '']">
									<text v-if="selectedIds.has(item.id)" class="indicator-check">&#10003;</text>
								</view>
							</view>
							<view class="play-badge" v-if="item.url">
								<text class="play-badge-icon">&#9654;</text>
							</view>
							<view class="duration-badge" v-if="item.duration">
								<text class="duration-text">{{ formatDuration(item.duration) }}</text>
							</view>
						</view>
						<view class="card-body">
							<!-- <text class="card-title">{{ item.title || item.id }}</text> -->
							<view class="card-stats">
								<text class="stat-item" v-if="item.pv">热度 {{ formatCount(item.pv) }}</text>
								<text class="stat-item" v-if="item.favorite">&#9829; {{ formatCount(item.favorite) }}</text>
							</view>
						</view>
					</view>
				</view>
			</view>

			<!-- 空状态 -->
			<view class="empty" v-if="!loading && !loadError && items.length === 0">
				<text class="empty-icon">&#128249;</text>
				<text class="empty-title">暂无内容</text>
				<text class="empty-desc">换个分类或筛选条件试试</text>
			</view>

			<!-- 加载更多 -->
			<view class="load-more" v-if="items.length > 0">
				<view class="spinner small" v-if="loadingMore"></view>
				<text class="load-more-text" v-if="loadingMore">加载中...</text>
				<text class="load-more-text end" v-else-if="!pagination.has_next">-- 已经到底了 --</text>
			</view>

			<!-- 底部安全区留白 -->
			<view class="bottom-spacer"></view>
		</scroll-view>

		<!-- 底部工具栏 -->
		<view class="toolbar" v-if="selectMode">
			<!-- 下载进度条 -->
			<view class="toolbar-progress" v-if="downloading">
				<view class="progress-bar-track">
					<view class="progress-bar-fill" :style="{ width: downloadProgress + '%' }"></view>
				</view>
				<view class="progress-info">
					<text class="progress-text">{{ downloadCurrentIndex }}/{{ downloadTotalCount }} 个文件</text>
					<text class="progress-percent">{{ downloadProgress }}%</text>
				</view>
			</view>
			<view class="toolbar-inner">
				<text class="toolbar-btn" @tap="toggleSelectMode">取消</text>
				<view class="toolbar-divider"></view>
				<text class="toolbar-btn" @tap="toggleSelectAll">
					{{ isAllSelected ? '取消全选' : '全选' }}
				</text>
				<view class="toolbar-divider"></view>
				<text class="toolbar-selected">已选 {{ selectedIds.size }} 项</text>
				<text
					:class="['toolbar-action', selectedIds.size === 0 && !downloading ? 'toolbar-action-disabled' : '']"
					@tap="downloadSelected"
				>
					{{ downloading ? '停止' : '下载' }}
				</text>
			</view>
		</view>

		<!-- 设置弹窗 iOS Sheet 风格 -->
		<view class="sheet-mask" :class="{ 'sheet-mask-visible': showSettings }" @tap="showSettings = false">
			<view class="sheet-container" :class="{ 'sheet-container-visible': showSettings }" @tap.stop>
				<view class="sheet-handle">
					<view class="handle-bar"></view>
				</view>
				<view class="sheet-header">
					<text class="sheet-title">设置</text>
				</view>
				<view class="sheet-body">
					<view class="setting-group">
						<picker :range="perPageOptions" @change="onPerPageChange" :value="perPageIndex">
							<view class="setting-row">
								<text class="setting-label">每页数量</text>
								<view class="setting-value-wrap">
									<text class="setting-value">{{ settingsForm.perPage }}</text>
									<text class="setting-arrow">&#8250;</text>
								</view>
							</view>
						</picker>
						<picker :range="sortLabels" @change="onSortChange" :value="settingsForm.sortIndex">
							<view class="setting-row">
								<text class="setting-label">排序方式</text>
								<view class="setting-value-wrap">
									<text class="setting-value">{{ currentSortLabel }}</text>
									<text class="setting-arrow">&#8250;</text>
								</view>
							</view>
						</picker>
						<picker :range="rangeLabels" @change="onRangeChange" :value="settingsForm.rangeIndex">
							<view class="setting-row">
								<text class="setting-label">时间范围</text>
								<view class="setting-value-wrap">
									<text class="setting-value">{{ currentRangeLabel }}</text>
									<text class="setting-arrow">&#8250;</text>
								</view>
							</view>
						</picker>
						<picker :range="timeFilterLabels" @change="onTimeFilterChange" :value="settingsForm.timeFilterIndex">
							<view class="setting-row last">
								<text class="setting-label">时长筛选</text>
								<view class="setting-value-wrap">
									<text class="setting-value">{{ currentTimeFilterLabel }}</text>
									<text class="setting-arrow">&#8250;</text>
								</view>
							</view>
						</picker>
					</view>
				</view>
				<view class="sheet-footer">
					<text class="sheet-btn-primary" @tap="saveSettings">完成</text>
				</view>
			</view>
		</view>
	</view>
</template>

<script>

	const ALL_TAGS = [
	{ code: 'kyonyu', name: '巨乳' },
{ code: 'creampie', name: '中出' },
{ code: 'uncensored', name: '无码' },
{ code: 'lolita', name: '萝莉' },
{ code: 'married-woman', name: '人妻' },
{ code: 'beautiful-girl', name: '美少女' },
{ code: 'masturbation', name: '自慰' },
{ code: 'shaved', name: '无毛' },
{ code: 'anal', name: '后门' },
{ code: 'facial', name: '颜射' },
{ code: 'small-breasts', name: '贫乳' },
{ code: 'jk', name: '女高中生' },
{ code: 'female-pervert', name: '痴女' },
{ code: 'gal', name: '辣妹' },
{ code: 'cum-swallowing', name: '吞精' },
{ code: 'fellatio', name: '口交' },
{ code: 'handjob', name: '手交' },
{ code: 'titjob', name: '乳交' },
{ code: 'deep-throat', name: '深喉' },
{ code: 'bukkake', name: '颜面' },
{ code: 'shirouto', name: '素人' },
{ code: 'incest', name: '乱伦' },
{ code: 'rape', name: '强奸' },
{ code: 'molestation', name: '痴汉' },
{ code: 'orgy', name: '乱交' },
{ code: 'outdoor', name: '户外' },
{ code: 'voyeur', name: '偷拍' },
{ code: 'pickup', name: '搭讪' },
{ code: 'cosplay', name: 'cosplay' },
{ code: 'anime', name: '二次元' },
{ code: 'sm', name: 'SM' },
{ code: 'hamedori', name: '自拍' },
{ code: 'personal-filming', name: '私人' },
{ code: 'female-teacher', name: '女教师' },
{ code: 'nurse', name: '护士' },
{ code: 'big-sister', name: '姐姐' },
{ code: 'swimsuit', name: '泳装' },
{ code: 'special-feature', name: '企划' },
{ code: 'massage', name: '按摩' },
{ code: 'gay', name: 'gay/伪娘' },
{ code: 'ja', name: '日本' },
{ code: 'zh-CN', name: '中国' },
{ code: 'th', name: '泰国' },
{ code: 'en', name: '英语' },
{ code: 'zh-TW', name: '繁体' },
{ code: 'ko', name: '韩语' },
{ code: 'id', name: '印尼' },
{ code: 'pt', name: '葡萄牙' },
{ code: 'fr', name: '法语' },
{ code: 'de', name: '德语' },
	];

	export default {
		data() {
			return {
				tabs: [{ code: '', name: '全部' }],
				currentTag: '',
				currentPage: 1,
				pagination: { page: 1, per_page: 10, has_next: false },
				items: [],
				selectMode: false,
				selectedIds: new Set(),
				downloadedIds: new Set(),
				loading: false,
				loadingMore: false,
				refreshing: false,
				loadError: '',
				showSettings: false,
				downloading: false,
				downloadProgress: 0,
				downloadCurrentIndex: 0,
				downloadTotalCount: 0,
				config: {
					per_page: 10,
					sort: 'pv',
					range: 'daily',
					min_time: 0,
					max_time: 86400,
				},
				settingsForm: {
					perPage: 10,
					sortIndex: 3,
					rangeIndex: 0,
					timeFilterIndex: 0,
				},
				perPageOptions: [10, 20, 30, 50, 100],
				sortOptions: [
					{ value: 'created', label: '最近添加' },
					{ value: 'time', label: '按时长' },
					{ value: 'favorite', label: '按点赞' },
					{ value: 'pv', label: '按观看数' },
				],
				rangeOptions: [
					{ value: 'daily', label: '每日' },
					{ value: 'weekly', label: '每周' },
					{ value: 'monthly', label: '每月' },
					{ value: 'all', label: '全部' },
				],
				timeFilterOptions: [
					{ label: '全部', min: 0, max: 86400 },
					{ label: '0-5分钟', min: 0, max: 300 },
					{ label: '5-15分钟', min: 300, max: 900 },
					{ label: '15-30分钟', min: 900, max: 1800 },
					{ label: '30分钟-1小时', min: 1800, max: 3600 },
					{ label: '一小时以上', min: 3600, max: 86400 },
				],
			};
		},
		computed: {
			isAllSelected() {
				return this.items.length > 0 && this.items.every(item => this.selectedIds.has(item.id));
			},
			leftItems() {
				return this.items.filter((_, i) => i % 2 === 0);
			},
			rightItems() {
				return this.items.filter((_, i) => i % 2 === 1);
			},
			sortLabels() {
				return this.sortOptions.map(o => o.label);
			},
			rangeLabels() {
				return this.rangeOptions.map(o => o.label);
			},
			timeFilterLabels() {
				return this.timeFilterOptions.map(o => o.label);
			},
			currentSortLabel() {
				return this.sortOptions[this.settingsForm.sortIndex]?.label || '按观看数';
			},
			currentRangeLabel() {
				return this.rangeOptions[this.settingsForm.rangeIndex]?.label || '每日';
			},
			currentTimeFilterLabel() {
				return this.timeFilterOptions[this.settingsForm.timeFilterIndex]?.label || '全部';
			},
			perPageIndex() {
				return this.perPageOptions.indexOf(this.settingsForm.perPage);
			},
			previewVideoList() {
				return this.items.map(item => ({
					src: item.url,
					description: item.title || item.id,
					poster: item.thumbnail || '',
					objectFit: 'cover',
					userAvatar: null,
					userName: item.title || item.id,
					userFollow: null,
				}));
			},
		},
		onLoad() {
			this.restoreConfig();
			this.loadDownloadedIds();
			this.tabs = [{ code: '', name: '全部' }, ...ALL_TAGS.map(t => ({ code: t.code, name: t.name }))];
			this.loadData();
		},
		onShow() {
			this.applyProxy();
			this.loadDownloadedIds();
		},
		onBackPress() {
			if (this.showSettings) {
				this.showSettings = false;
				return true;
			}
			return false;
		},
		methods: {
			goToFiles() {
				uni.navigateTo({ url: '/pages/files/files' });
			},
			goToProxy() {
				uni.navigateTo({ url: '/pages/proxy/proxy' });
			},
			loadDownloadedIds() {
				try {
					const records = JSON.parse(uni.getStorageSync('download_records') || '[]');
					this.downloadedIds = new Set(records.map(r => r.id).filter(Boolean));
				} catch (e) {
					this.downloadedIds = new Set();
				}
			},
			formatDuration(seconds) {
				const s = Number(seconds);
				if (!s || s <= 0) return '';
				if (s < 60) return `${s}秒`;
				if (s < 3600) {
					const m = Math.floor(s / 60);
					const sec = s % 60;
					return sec > 0 ? `${m}:${String(sec).padStart(2, '0')}` : `${m}分钟`;
				}
				const h = Math.floor(s / 3600);
				const m = Math.floor((s % 3600) / 60);
				return `${h}:${String(m).padStart(2, '0')}`;
			},
			formatCount(num) {
				const n = Number(num);
				if (!n) return '';
				if (n >= 10000) return (n / 10000).toFixed(1) + 'w';
				if (n >= 1000) return (n / 1000).toFixed(1) + 'k';
				return String(n);
			},

			restoreConfig() {
				try {
					const saved = uni.getStorageSync('waterfall_config');
					if (saved) {
						const parsed = JSON.parse(saved);
						this.config = { ...this.config, ...parsed };
					}
				} catch (e) {}
				this.syncSettingsForm();
			},
			persistConfig() {
				try {
					uni.setStorageSync('waterfall_config', JSON.stringify(this.config));
				} catch (e) {}
			},
			applyProxy() {
				if (typeof plus === 'undefined') return;
				let host = '';
				let port = 80;
				try {
					const data = uni.getStorageSync('proxy_config');
					if (data) {
						const parsed = JSON.parse(data);
						if (parsed.enabled && parsed.selectedId) {
							const scheme = (parsed.schemes || []).find(s => s.id === parsed.selectedId);
							if (scheme) {
								host = scheme.host;
								port = scheme.port;
							}
						}
					}
				} catch (e) {}

				try {
					const System = plus.android.importClass('java.lang.System');
					if (host) {
						System.setProperty('http.proxyHost', host);
						System.setProperty('http.proxyPort', String(port));
						System.setProperty('https.proxyHost', host);
						System.setProperty('https.proxyPort', String(port));
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
			syncSettingsForm() {
				this.settingsForm.perPage = this.config.per_page || 10;
				this.settingsForm.sortIndex = this.sortOptions.findIndex(o => o.value === this.config.sort);
				if (this.settingsForm.sortIndex < 0) this.settingsForm.sortIndex = 3;
				this.settingsForm.rangeIndex = this.rangeOptions.findIndex(o => o.value === this.config.range);
				if (this.settingsForm.rangeIndex < 0) this.settingsForm.rangeIndex = 0;
				this.settingsForm.timeFilterIndex = this.timeFilterOptions.findIndex(
					o => o.min === this.config.min_time && o.max === this.config.max_time
				);
				if (this.settingsForm.timeFilterIndex < 0) this.settingsForm.timeFilterIndex = 0;
			},

			buildMediaParams(page) {
				const cfg = this.config;
				const params = {
					page: Math.max(1, page),
					per_page: cfg.per_page,
					ids: '',
					isAnimeOnly: 0,
					sort: cfg.sort,
				};
				if (cfg.min_time > 0) params.min_time = cfg.min_time;
				if (cfg.max_time < 86400) params.max_time = cfg.max_time;
				if (this.currentTag) params.category = this.currentTag;
				if (cfg.range !== 'daily') params.range = cfg.range;
				return params;
			},
			normalizeItem(item) {
				if (!item || typeof item !== 'object') return null;
				const id = String(item.id || '').trim();
				const url = String(item.url || '').trim();
				const thumbnail = String(item.thumbnail || '').trim();
				if (!id || !url) return null;
				if (!url.startsWith('http://') && !url.startsWith('https://')) return null;
				return {
					id,
					url,
					thumbnail: thumbnail.startsWith('http') ? thumbnail : '',
					title: String(item.title || item.name || id),
					duration: item.duration || item.time || '',
					favorite: item.favorite || item.favorites || '',
					pv: item.pv || item.views || '',
				};
			},
			async fetchMedia(page) {
				const params = this.buildMediaParams(page);
				const res = await new Promise((resolve, reject) => {
					uni.request({
						url: 'https://truvaze.com/api/media',
						method: 'GET',
						data: params,
						timeout: 30000,
						success: resolve,
						fail: reject,
					});
				});
				if (res.statusCode !== 200) throw new Error(`请求失败 (${res.statusCode})`);
				const payload = res.data;
				if (!payload || !Array.isArray(payload.items)) throw new Error('数据格式异常');
				const items = payload.items.map(i => this.normalizeItem(i)).filter(Boolean);
				const has_next = payload.items.length >= params.per_page;
				return { items, has_next };
			},

			async loadData() {
				if (this.loading) return;
				this.loading = true;
				this.loadError = '';
				this.selectedIds = new Set();
				try {
					const result = await this.fetchMedia(this.currentPage);
					this.items = result.items;
					this.pagination = { page: this.currentPage, per_page: this.config.per_page, has_next: result.has_next };
				} catch (e) {
					this.items = [];
					this.loadError = e.message || '加载失败';
				} finally {
					this.loading = false;
				}
			},
			async loadMore() {
				if (this.loadingMore || !this.pagination.has_next || this.loading) return;
				this.loadingMore = true;
				const nextPage = this.currentPage + 1;
				try {
					const result = await this.fetchMedia(nextPage);
					this.items = [...this.items, ...result.items];
					this.currentPage = nextPage;
					this.pagination = { page: nextPage, per_page: this.config.per_page, has_next: result.has_next };
				} catch (e) {
					uni.showToast({ title: '加载失败', icon: 'none' });
				} finally {
					this.loadingMore = false;
				}
			},
			async onRefresh() {
				this.refreshing = true;
				this.currentPage = 1;
				await this.loadData();
				this.refreshing = false;
			},
			switchTab(code) {
				if (code === this.currentTag || this.loading) return;
				this.currentTag = code;
				this.currentPage = 1;
				this.pagination.has_next = false;
				this.loadData();
			},
			toggleSelect(id) {
				if (!this.selectMode) return;
				const newSet = new Set(this.selectedIds);
				if (newSet.has(id)) newSet.delete(id);
				else newSet.add(id);
				this.selectedIds = newSet;
			},
			toggleSelectMode() {
				this.selectMode = !this.selectMode;
				if (!this.selectMode) {
					this.selectedIds = new Set();
				}
			},
			toggleSelectAll() {
				const newSet = new Set(this.selectedIds);
				if (this.isAllSelected) this.items.forEach(item => newSet.delete(item.id));
				else this.items.forEach(item => newSet.add(item.id));
				this.selectedIds = newSet;
			},

			onCardTap(item) {
				if (this.selectMode) {
					this.toggleSelect(item.id);
					return;
				}
				const index = this.items.findIndex(candidate => candidate.id === item.id);
				this.openPreview(index >= 0 ? index : 0);
			},
			openPreview(index) {
				if (!this.items.length) return;
				const list = this.previewVideoList;
				uni.navigateTo({
					url: '/pages/play/play',
					success: (res) => {
						res.eventChannel.emit('initData', { list, index });
					},
				});
			},

			async downloadSelected() {
				if (this.downloading) {
					this.cancelDownload = true;
					if (this._currentDownloadTask) {
						this._currentDownloadTask.abort();
						this._currentDownloadTask = null;
					}
					return;
				}
				if (this.selectedIds.size === 0) return;
				const selectedItems = this.items.filter(item => this.selectedIds.has(item.id));
				if (!selectedItems.length) return;
				this.downloading = true;
				this.cancelDownload = false;
				this.downloadTotalCount = selectedItems.length;
				this.downloadCurrentIndex = 0;
				this.downloadProgress = 0;
				let success = 0;
				let failed = 0;
				for (let i = 0; i < selectedItems.length; i++) {
					if (this.cancelDownload) break;
					const item = selectedItems[i];
					this.downloadCurrentIndex = i + 1;
					this.downloadProgress = 0;
					try {
						await this.downloadOneFile(item);
						success++;
					} catch (e) {
						if (this.cancelDownload) break;
						failed++;
					}
				}
				this.downloading = false;
				this.downloadProgress = 0;
				this.downloadCurrentIndex = 0;
				this.downloadTotalCount = 0;
				this.cancelDownload = false;
				if (this.selectedIds.size > 0) this.selectedIds = new Set();
				if (failed > 0) {
					uni.showToast({ title: failed + ' 个下载失败', icon: 'none' });
				}
			},
			downloadOneFile(item) {
				return new Promise((resolve, reject) => {
					if (!item.url) { reject(new Error('无下载地址')); return; }
					const task = uni.downloadFile({
						url: item.url,
						success: (res) => {
							if (this.cancelDownload) { resolve(); return; }
							if (res.statusCode !== 200) { reject(new Error(`下载失败`)); return; }
							uni.saveFile({
								tempFilePath: res.tempFilePath,
								success: (saveRes) => {
									this.saveDownloadRecord(item, saveRes.savedFilePath);
									resolve();
								},
								fail: () => {
									uni.saveVideoToPhotosAlbum({
										filePath: res.tempFilePath,
										success: () => {
											this.saveDownloadRecord(item, res.tempFilePath);
											resolve();
										},
										fail: () => resolve(),
									});
								},
							});
						},
						fail: (err) => {
							if (this.cancelDownload) { resolve(); return; }
							reject(new Error(err.errMsg || '下载失败'));
						},
					});
					this._currentDownloadTask = task;
					task.onProgressUpdate((res) => {
						this.downloadProgress = res.progress;
					});
				});
			},
			saveDownloadRecord(item, filePath) {
				try {
					const records = JSON.parse(uni.getStorageSync('download_records') || '[]');
					records.unshift({
						id: item.id,
						title: item.title || item.id,
						thumbnail: item.thumbnail || '',
						url: item.url,
						filePath: filePath,
						downloadedAt: Date.now(),
					});
					uni.setStorageSync('download_records', JSON.stringify(records));
					this.downloadedIds.add(item.id);
				} catch (e) {}
			},

			onPerPageChange(e) {
				this.settingsForm.perPage = this.perPageOptions[e.detail.value];
			},
			onSortChange(e) {
				this.settingsForm.sortIndex = e.detail.value;
			},
			onRangeChange(e) {
				this.settingsForm.rangeIndex = e.detail.value;
			},
			onTimeFilterChange(e) {
				this.settingsForm.timeFilterIndex = e.detail.value;
			},
			saveSettings() {
				const sort = this.sortOptions[this.settingsForm.sortIndex];
				const range = this.rangeOptions[this.settingsForm.rangeIndex];
				const timeFilter = this.timeFilterOptions[this.settingsForm.timeFilterIndex];
				this.config = {
					per_page: this.settingsForm.perPage,
					sort: sort.value,
					range: range.value,
					min_time: timeFilter.min,
					max_time: timeFilter.max,
				};
				this.persistConfig();
				this.showSettings = false;
				this.currentPage = 1;
				this.pagination.page = 1;
				this.pagination.has_next = false;
				this.loadData();
			},
		},
	};
</script>

<style scoped>
	/* ===== ProHub Dark Design System ===== */
	.page {
		display: flex;
		flex-direction: column;
		height: 100vh;
		background-color: #0C0A09;
	}

	/* ===== 大标题导航栏 ===== */
	.nav-bar {
		background-color: #0C0A09;
		padding-top: var(--status-bar-height, 44px);
	}
	.nav-content {
		padding: 8rpx 32rpx 16rpx;
	}
	.nav-header-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}
	.nav-actions {
		display: flex;
		gap: 16rpx;
	}
	.nav-btn {
		width: 76rpx;
		height: 76rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		border-radius: 50%;
		background-color: #1C1917;
		border: 1rpx solid #44403C;
	}
	.nav-btn-icon {
		width: 44rpx;
		height: 44rpx;
	}
	.nav-large-title {
		font-size: 68rpx;
		font-weight: 700;
		color: #EAB308;
		letter-spacing: -1rpx;
		line-height: 1.1;
	}

	/* ===== 分类标签栏 ===== */
	.tab-scroll {
		white-space: nowrap;
		padding: 0 32rpx 24rpx;
		background-color: #0C0A09;
	}
	.tab-scroll::-webkit-scrollbar {
		display: none;
	}
	.tab-track {
		display: flex;
		gap: 16rpx;
	}
	.tab-btn {
		display: inline-flex;
		flex-shrink: 0;
		flex-direction: row;
		align-items: center;
		justify-content: center;
		white-space: nowrap;
		height: 60rpx;
		padding: 0 32rpx;
		border-radius: 30rpx;
		background-color: #1C1917;
		border: 1rpx solid #44403C;
		transition: all 0.25s cubic-bezier(0.32, 0.72, 0, 1);
	}
	.tab-btn-text {
		font-size: 26rpx;
		font-weight: 500;
		color: #A8A29E;
		letter-spacing: 0.3rpx;
		white-space: nowrap;
	}
	.tab-btn-active {
		background-color: #EAB308;
		border-color: #EAB308;
	}
	.tab-btn-active .tab-btn-text {
		color: #0C0A09;
		font-weight: 600;
	}

	/* ===== 状态提示 ===== */
	.status-hint {
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 32rpx;
		gap: 12rpx;
	}
	.status-hint.error .status-hint-text {
		color: #EF4444;
	}
	.status-hint-text {
		font-size: 28rpx;
		color: #A8A29E;
	}
	.spinner {
		width: 32rpx;
		height: 32rpx;
		border: 4rpx solid rgba(234, 179, 8, 0.2);
		border-top-color: #EAB308;
		border-radius: 50%;
		animation: spin 0.8s linear infinite;
	}
	.spinner.small {
		width: 24rpx;
		height: 24rpx;
		border-width: 3rpx;
	}
	@keyframes spin {
		to { transform: rotate(360deg); }
	}

	/* ===== 瀑布流 ===== */
	.waterfall-scroll {
		flex: 1;
		overflow: hidden;
	}
	.waterfall-container {
		display: flex;
		padding: 0 24rpx;
		gap: 16rpx;
	}
	.waterfall-col {
		flex: 1;
		display: flex;
		flex-direction: column;
		gap: 16rpx;
	}
	.bottom-spacer {
		height: 140rpx;
	}

	/* ===== 卡片 ===== */
	.card {
		border-radius: 20rpx;
		background-color: #1C1917;
		overflow: hidden;
		border: 1rpx solid #44403C;
		transition: transform 0.15s ease, box-shadow 0.15s ease;
	}
	.card-selected {
		box-shadow: 0 0 0 4rpx rgba(234, 179, 8, 0.4), 0 2rpx 12rpx rgba(0, 0, 0, 0.3);
		border-color: #EAB308;
	}

	/* ===== 海报 ===== */
	.poster {
		position: relative;
		min-height: 240rpx;
		background-color: #292524;
		overflow: hidden;
	}
	.poster-img {
		width: 100%;
		display: block;
	}
	.poster-fallback {
		min-height: 320rpx;
		display: flex;
		align-items: center;
		justify-content: center;
	}
	.fallback-icon {
		color: rgba(255, 255, 255, 0.25);
		font-size: 64rpx;
	}

	/* 已下载标志 */
	.downloaded-badge {
		position: absolute;
		top: 0;
		right: 0;
		z-index: 2;
		width: 72rpx;
		height: 72rpx;
	}

	/* 选中指示器 */
	.select-indicator {
		position: absolute;
		top: 16rpx;
		left: 16rpx;
		z-index: 2;
		padding: 4rpx;
	}
	.indicator-circle {
		width: 44rpx;
		height: 44rpx;
		border-radius: 50%;
		border: 3rpx solid rgba(255, 255, 255, 0.9);
		background-color: rgba(0, 0, 0, 0.3);
		display: flex;
		align-items: center;
		justify-content: center;
		transition: all 0.15s ease;
	}
	.indicator-checked {
		background-color: #EAB308;
		border-color: #EAB308;
	}
	.indicator-check {
		color: #0C0A09;
		font-size: 24rpx;
		font-weight: 700;
	}

	/* 播放按钮 */
	.play-badge {
		position: absolute;
		right: 16rpx;
		bottom: 16rpx;
		width: 56rpx;
		height: 56rpx;
		border-radius: 50%;
		background-color: rgba(0, 0, 0, 0.55);
		display: flex;
		align-items: center;
		justify-content: center;
	}
	.play-badge-icon {
		color: #EAB308;
		font-size: 22rpx;
		margin-left: 4rpx;
	}

	/* 时长标签 */
	.duration-badge {
		position: absolute;
		left: 16rpx;
		bottom: 16rpx;
		padding: 4rpx 12rpx;
		border-radius: 8rpx;
		background-color: rgba(0, 0, 0, 0.65);
	}
	.duration-text {
		font-size: 20rpx;
		color: #FAFAF9;
		font-weight: 500;
	}

	/* ===== 卡片内容 ===== */
	.card-body {
		padding: 16rpx 20rpx 20rpx;
	}
	.card-title {
		font-size: 26rpx;
		font-weight: 600;
		color: #FAFAF9;
		line-height: 1.45;
		display: -webkit-box;
		-webkit-box-orient: vertical;
		-webkit-line-clamp: 2;
		overflow: hidden;
	}
	.card-stats {
		display: flex;
		gap: 16rpx;
		margin-top: 10rpx;
	}
	.stat-item {
		font-size: 22rpx;
		color: #A8A29E;
		font-weight: 400;
	}

	/* ===== 空状态 ===== */
	.empty {
		padding: 120rpx 40rpx;
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

	/* ===== 加载更多 ===== */
	.load-more {
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 32rpx 0;
		gap: 12rpx;
	}
	.load-more-text {
		font-size: 26rpx;
		color: #A8A29E;
	}
	.load-more-text.end {
		font-size: 24rpx;
		color: #57534E;
	}

	/* ===== 底部工具栏 ===== */
	.toolbar {
		position: fixed;
		left: 0;
		right: 0;
		bottom: 0;
		z-index: 50;
		background-color: rgba(28, 25, 23, 0.95);
		backdrop-filter: blur(20px);
		border-top: 1rpx solid #44403C;
	}
	.toolbar-inner {
		display: flex;
		align-items: center;
		padding: 16rpx 32rpx;
		padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
		gap: 20rpx;
	}
	.toolbar-btn {
		font-size: 28rpx;
		color: #EAB308;
		font-weight: 500;
	}
	.toolbar-divider {
		width: 1rpx;
		height: 32rpx;
		background-color: #44403C;
	}
	.toolbar-selected {
		flex: 1;
		font-size: 26rpx;
		color: #A8A29E;
	}
	.toolbar-action {
		font-size: 28rpx;
		color: #0C0A09;
		font-weight: 600;
		padding: 14rpx 36rpx;
		border-radius: 20rpx;
		background-color: #EAB308;
	}
	.toolbar-action-disabled {
		background-color: #57534E;
		opacity: 0.6;
		color: #A8A29E;
	}

	/* ===== 下载进度条 ===== */
	.toolbar-progress {
		padding: 16rpx 32rpx 0;
	}
	.progress-bar-track {
		height: 8rpx;
		border-radius: 4rpx;
		background-color: rgba(234, 179, 8, 0.15);
		overflow: hidden;
	}
	.progress-bar-fill {
		height: 100%;
		border-radius: 4rpx;
		background-color: #EAB308;
		transition: width 0.3s ease;
	}
	.progress-info {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-top: 8rpx;
	}
	.progress-text {
		font-size: 22rpx;
		color: #A8A29E;
	}
	.progress-percent {
		font-size: 22rpx;
		color: #EAB308;
		font-weight: 600;
	}

	/* ===== Sheet 弹窗 ===== */
	.sheet-mask {
		position: fixed;
		inset: 0;
		background-color: rgba(0, 0, 0, 0);
		z-index: 100;
		transition: background-color 0.3s ease;
		pointer-events: none;
	}
	.sheet-mask-visible {
		background-color: rgba(0, 0, 0, 0.6);
		pointer-events: auto;
	}
	.sheet-container {
		position: fixed;
		left: 0;
		right: 0;
		bottom: 0;
		max-height: 80vh;
		background-color: #1C1917;
		border-radius: 28rpx 28rpx 0 0;
		transform: translateY(100%);
		transition: transform 0.35s cubic-bezier(0.32, 0.72, 0, 1);
		overflow: hidden;
	}
	.sheet-container-visible {
		transform: translateY(0);
	}
	.sheet-handle {
		display: flex;
		justify-content: center;
		padding: 16rpx 0 8rpx;
	}
	.handle-bar {
		width: 72rpx;
		height: 8rpx;
		border-radius: 4rpx;
		background-color: #57534E;
	}
	.sheet-header {
		padding: 8rpx 40rpx 20rpx;
	}
	.sheet-title {
		font-size: 34rpx;
		font-weight: 700;
		color: #FAFAF9;
	}
	.sheet-body {
		padding: 0 32rpx;
	}
	.setting-group {
		background-color: #292524;
		border-radius: 20rpx;
		overflow: hidden;
	}
	.setting-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 28rpx 32rpx;
		border-bottom: 1rpx solid #44403C;
	}
	.setting-row.last {
		border-bottom: none;
	}
	.setting-label {
		font-size: 30rpx;
		color: #FAFAF9;
	}
	.setting-value-wrap {
		display: flex;
		align-items: center;
		gap: 8rpx;
	}
	.setting-value {
		font-size: 30rpx;
		color: #A8A29E;
	}
	.setting-arrow {
		font-size: 36rpx;
		color: #57534E;
		font-weight: 300;
	}
	.setting-input {
		flex: 1;
		font-size: 28rpx;
		color: #FAFAF9;
		text-align: right;
		padding: 0;
		margin: 0;
		background: transparent;
	}
	.setting-input-placeholder {
		color: #57534E;
		font-size: 26rpx;
	}
	.sheet-footer {
		padding: 24rpx 32rpx;
		padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
	}
	.sheet-btn-primary {
		display: block;
		text-align: center;
		font-size: 34rpx;
		font-weight: 600;
		color: #0C0A09;
		padding: 24rpx 0;
		border-radius: 20rpx;
		background-color: #EAB308;
	}
</style>
