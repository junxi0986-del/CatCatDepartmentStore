import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

const app = createApp(App)

// 创建事件总线
app.config.globalProperties.$bus = app

app.use(router)
app.use(ElementPlus)
app.mount('#app')
