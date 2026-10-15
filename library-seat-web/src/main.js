import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import * as Icons from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import './style.css'
import App from './App.vue'

const app=createApp(App)
app.use(ElementPlus)
Object.entries(Icons).forEach(([key,component])=>app.component(key,component))
app.mount('#app')
