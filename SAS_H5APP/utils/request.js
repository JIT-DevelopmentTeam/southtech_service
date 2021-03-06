import axios from 'axios'

export const http = axios.create({
	// baseURL: 'http://localhost:8080/jeecg-boot',
	// baseURL: 'http://120.77.40.245:8080/sac',// 捷特
	baseURL: 'http://40.73.75.101:8080/jeecg-boot',// 索奥斯
	timeout: 50000,
	headers: {
	  'Accept': 'application/json',  
	  'Content-Type': 'application/json'  
	},
	withCredentials: true
})

// Add a request interceptor
http.interceptors.request.use(function (config) {
    // Do something before request is sent
	uni.showLoading({
	    title: '加载中',
		mask: true
	});
	config.headers.common['code'] = sessionStorage.getItem('code')
    return config;
  }, function (error) {
    // Do something with request error
    return Promise.reject(error);
  });

// Add a response interceptor
http.interceptors.response.use(function (response) {
    // Any status code that lie within the range of 2xx cause this function to trigger
    // Do something with response data
	uni.hideLoading();
    return response;
  }, function (error) {
    // Any status codes that falls outside the range of 2xx cause this function to trigger
    // Do something with response error
	uni.showToast({
	    title: error.toString(),
	    duration: 2000,
		icon: 'none'
	});
    return Promise.reject(error);
  });