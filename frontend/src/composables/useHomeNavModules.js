import { ref } from 'vue'
import api from '../services/api'

const navModules = ref([])
let loadingPromise = null

export function useHomeNavModules() {
  async function loadNavModules(force = false) {
    if (!force && navModules.value.length) {
      return navModules.value
    }
    if (!loadingPromise || force) {
      loadingPromise = api.get('/home/nav-modules')
        .then((data) => {
          navModules.value = data || []
          return navModules.value
        })
        .catch(() => {
          navModules.value = []
          return navModules.value
        })
        .finally(() => {
          loadingPromise = null
        })
    }
    return loadingPromise
  }

  function isModuleVisible(code, modules = navModules.value) {
    return modules.some((item) => item.code === code)
  }

  return {
    navModules,
    loadNavModules,
    isModuleVisible,
  }
}
