import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import App from './App.vue'
import router from './router'
import './assets/styles.css'
import { defocusDocument } from './utils/defocusDocument'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')

defocusDocument()
requestAnimationFrame(defocusDocument)
router.isReady().then(defocusDocument)
router.afterEach(() => requestAnimationFrame(defocusDocument))
