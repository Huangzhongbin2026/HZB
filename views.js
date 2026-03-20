/**
 * 视图注册声明
 * @type {Object}
 * @description
 * 键：'/views/About/index.js' -> 代表模块输出路径
 * 值：'./src/views/About/index.vue' -> 代表模块源码路径
 * 用：'/_' 直接在浏览器上输入访问可在本地开发时访问所有注册的模块
 */
module.exports = {
  '/index.js': './src/index.ts', // 页面入口
  '/views/SupplyTask/index.js?label=供应链统筹任务管理平台': './src/views/SupplyTask/index.vue',
  // '/views/HelloWorld/index.js?label=你好世界': './src/views/HelloWorld/index.vue'
  示例: require('@one/demos/views.js'), // node_modules/@one/demos/src包含示例源码
}
