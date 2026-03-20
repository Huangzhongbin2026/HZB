import './index.css'
import core, { views, icons } from '@one/core'

core.init(
  // 勾子 - hooks
  {},
  // 插件 - plugins
  [
    views(require('@/../views.js')), // 支持开发环境通过 /_ 访问所有视图
    icons(import.meta.webpackContext('@/icons')), // 支持 <Icon icon="core:{文件名}" />
  ],
)
