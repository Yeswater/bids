import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'vxe-table/lib/style.css'
import VXETable from 'vxe-table'
import App from './App.vue'
import './style.css'

const app = createApp(App)
app.use(ElementPlus)
app.use(VXETable)
app.mount('#app')
