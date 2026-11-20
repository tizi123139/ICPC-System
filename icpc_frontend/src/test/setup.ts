import '@testing-library/jest-dom/vitest'
import { config } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    config.global.components[key] = component
}

config.global.plugins = [[ElementPlus, { locale: zhCn }]]
