<template>
	<view class="content">
		<uni-segmented-control class="borderStyle" :current="current" :values="items" style-type="button" active-color="#09a0f7" @clickItem="onClickItem"></uni-segmented-control>
		<scroll-view :scroll-top="scrollTop" scroll-y="true" class="scroll-Y" @scrolltoupper="upper" @scrolltolower="lower" @scroll="scroll">
			<uni-swipe-action id="swipe">
				<work-order-list v-for="(item,index) in dataSource" :info="item" :key="index" :current="current"></work-order-list>
			</uni-swipe-action>
			<uni-load-more :status="'loading'" v-show="load"></uni-load-more>
		</scroll-view>
	</view>
</template>

<script>
	export default {
		name: 'subSection',
		components: {
			uniSegmentedControl: () => import('@dcloudio/uni-ui/lib/uni-segmented-control/uni-segmented-control.vue'), 
			workOrderList: () => import('./component/work-order-list.vue'),
			uniLoadMore: () => import('@dcloudio/uni-ui/lib/uni-load-more/uni-load-more.vue'),
			uniSwipeAction: () => import('@dcloudio/uni-ui/lib/uni-swipe-action/uni-swipe-action.vue')
		},
		data() {
			return {
				items: ['未完工','已完工','全部'],
				current: 0,
				scrollTop: 0,
				old: {
					scrollTop: 0
				},
				load: false,
				pageNo: 1,
				pageSize: 5,
				dataSource: [],
				total: 0
			}
		},
		methods: {
			/**
			 * 点击segmentedControl 事件回调
			 */
			onClickItem(e) {
				if (this.current !== e.currentIndex) {
					this.current = e.currentIndex;
					this.dataSource = [];
					this.pageNo = 1;
					let obj = {
						openId: this.$store.getters['getWeChatOpenId'],
						status: this.current,
						pageNo: this.pageNo,
						pageSize: this.pageSize
					}
					this.loadTicketList(obj)
				}
			},
			upper() {
				// console.log("到顶了");
			},
			lower() {
				// console.log("到底了");
				if (this.dataSource.length != this.total) {
					this.load = true;
					this.pageNo += 1;
					let obj = {
						openId: this.$store.getters['getWeChatOpenId'],
						status: this.current,
						pageNo: this.pageNo,
						pageSize: this.pageSize
					}
					this.loadTicketList(obj)
				}
			},
			scroll() {
				// console.log("滚动了");
			},
			loadTicketList(obj) {
				this.$store.dispatch('workOrder/GetServiceDataList', obj).then(res=>{
					this.total = res.total
					let result = res.list
					result.forEach(item => {
						this.dataSource.push(item)
						this.load = false;
					})
					const dataList = this.dataSource.slice(0);
					this.$store.commit('workOrder/setServiceTicketList', dataList)
				});
			}
		},
		onLoad(option) {
			this.$store.dispatch('dic/GetServiceTypeList', 'work_order_type').then(res=>{
				let obj = {
					openId: this.$store.getters['getWeChatOpenId'],
					status: this.current,
					pageNo: this.pageNo,
					pageSize: this.pageSize
				}
				this.loadTicketList(obj)
			})
		}
	}
</script>

<style scoped>
	.borderStyle {
		margin: 15upx;
	}
	.borderStyle /deep/ .segmented-control__text {
		font-size: 0.6rem !important;
	}
	/* iphone X */
	@media only screen and (device-width: 375px) and (device-height: 812px) and (-webkit-device-pixel-ratio: 3) {
		.scroll-Y {
			height: calc(100vh - 180px - 36px - 100px);
		}
	}
	/* iphone 6~8 */
	@media only screen and (device-width : 375px) and (device-height : 667px) and (-webkit-device-pixel-ratio : 2) {
		.scroll-Y {
			height: calc(100vh - 180px - 36px - 70px);
		}
	}
	/* iphone 6 plus~8 plus */
	@media only screen and (device-width : 414px) and (device-height : 736px) and (-webkit-device-pixel-ratio : 3) {
		.scroll-Y {
			height: calc(100vh - 180px - 36px - 70px);
		}
	}
	.scroll-Y {
		height: calc(100vh - 29px - 60px);
	}
	#swipe /deep/ .uni-swipe_box {
		background-color: transparent !important;
	}
</style>
