<template>
	<view class=" chooseImage " style="display: flex;flex-wrap: wrap;">
		<view style="position: relative;" v-for="(item, index) in imgList" :key="index" :style="{ width: size + 'rpx', height: size + 'rpx' }">
			<image :src="imgList[index].path" :style="{ width: size + 'rpx', height: size + 'rpx' }" mode="aspectFill" @click="viewImg(imgList[index].path)"></image>
			<view v-show="stageStatus != 1">
				<view class="icon_close " style="position: absolute;" @click="delImg(index, imgList[index].id)">
					<i class="iconfont iconjiaocha" style=""></i>
				</view>
			</view>
		</view>
		<view v-show="stageStatus != 1">
			<view v-if="imgList.length < num" class="text_center" @click="chooseImage">
				<image src="/static/upload.png" mode="widthFix" :style="{ width: size + 'rpx' }"></image>
			</view>
		</view>
	</view>
</template>

<script>
	import {guid} from '@/utils/common.js'
	
export default {
	name:"chooseImage",
	props: {
		size: {
			//图片的尺寸
			type: Number,
			default: 200
		},
		num: {
			//上传图片数量
			type: Number,
			default: 9
		},
		isSave: {
			//是否记录用户的选择记录
			type: Boolean,
			default: true
		},
		saveStr: {
			//记录用户的缓存字段
			type: String,
			default: 'chooseImage'
		},
		isBase64: {
			//是否转base64 受数据传输长度限制，不建议在组件中使用，如果一定要使用，在返回结果中自己转换
			type: Boolean,
			default: false
		},
		imageList: {
			type: Array,
			default: []
		},
		stageStatus: {	// 阶段完成状态（为 1 就是已完成，只能查看；为 0 就是继续，可编辑）
			type: String,
			default: '0'
		},
		requestRUl:{
			type: String
		}
	},
	data() {
		return {
			imgList: [],
			base64: '',
		};
	},
	methods: {
		chooseImage: async function() {
			let _this = this;
			await _this.getImage();
		},
		getImage() {
			let _this = this;
			let _count = _this.num - _this.imgList.length;
			uni.chooseImage({
				count: _count, //默认9
				sizeType: ['original', 'compressed'], //可以指定是原图还是压缩图，默认二者都有
				sourceType: ['album', 'camera'], //从相册选择
				success: function(res) {
					for (var i = 0; i < res.tempFilePaths.length; i++) {
						var obj = {id: guid(), path: res.tempFilePaths[i]}
						_this.imgList.push(obj)
					}
					

					if (_this.isSave) {
						uni.setStorageSync(_this.saveStr, _this.imgList.join(','));
					}
					
					_this.$emit("uploadPhotoSuccess", _this.imgList)
				}
			});
		},
		delImg(idx, id) {
			uni.showModal({
				title: '提示',
				content: '确定要删除此项吗？',
				success: res => {
					if (res.confirm) {
						this.imgList.splice(idx, 1);
						this.$forceUpdate();
						this.$emit('deletePhotoSuccess', this.imgList, id);
						// this.$emit('update:photoList', this.imgList);
					}
				}
			});
			if (this.isSave) {
				uni.setStorageSync(this.saveStr, this.imgList.join(','));
			}
		},
		viewImg(path) {
			var urls = []
			for (var i = 0; i < this.imgList.length; i++) {
				urls.push(this.imgList[i].path)
			}
			uni.previewImage({
				urls: urls,
				current: path
			});
		}
	},
	mounted() {
		this.imgList = this.imageList
		if (this.isSave) {
			let str = uni.getStorageSync(this.saveStr);
			if (str != '') {
				str = str.split(',');
				if (str.length > this.num) {
					str = str.slice(0, this.num);
				}
				this.imgList = str;
			}
		} else {
			uni.removeStorageSync(this.saveStr);
		}
	}
};
</script>

<style lang="less" scoped="">
.iconfont {
	font-family: 'iconfont' !important;
	font-size: 20px;
	font-style: normal;
	-webkit-font-smoothing: antialiased;
	-webkit-text-stroke-width: 0.2px;
	-moz-osx-font-smoothing: grayscale;
	cursor: pointer;
}
.chooseImage {
	> view {
		margin: 0 20upx 20upx 0;
	}
	.icon_close {
		right: 0;
		top: 0;
		.iconfont {
			background: rgba(0, 0, 0, 0.4);
			color: #fff;
			border-radius: 50%;
			padding: 8upx;
			font-size: 20upx;
		}
	}
}
</style>
