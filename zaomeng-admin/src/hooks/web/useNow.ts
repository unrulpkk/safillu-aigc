import { dateUtil } from '@/utils/dateUtil'
import { reactive, toRefs, watch } from 'vue'
import { useLocaleStore } from '@/store/modules/locale'
import { tryOnMounted, tryOnUnmounted } from '@vueuse/core'

export const useNow = (immediate = true) => {
  const localeStore = useLocaleStore()
  let timer: IntervalHandle

  const state = reactive({
    year: 0,
    month: 0,
    week: '',
    day: 0,
    hour: '',
    minute: '',
    second: 0,
    meridiem: ''
  })

  const update = () => {
    const now = dateUtil()

    const h = now.format('HH')
    const m = now.format('mm')
    const s = now.get('s')

    state.year = now.get('y')
    state.month = now.get('M') + 1
    state.week = new Intl.DateTimeFormat(localeStore.currentLocale.lang, {
      weekday: 'long'
    }).format(new Date())
    state.day = now.get('date')
    state.hour = h
    state.minute = m
    state.second = s

    state.meridiem =
      new Intl.DateTimeFormat(localeStore.currentLocale.lang, { hour: 'numeric', hour12: true })
        .formatToParts(new Date())
        .find((part) => part.type === 'dayPeriod')?.value || ''
  }

  watch(() => localeStore.currentLocale.lang, update)

  function start() {
    update()
    clearInterval(timer)
    timer = setInterval(() => update(), 1000)
  }

  function stop() {
    clearInterval(timer)
  }

  tryOnMounted(() => {
    immediate && start()
  })

  tryOnUnmounted(() => {
    stop()
  })

  return {
    ...toRefs(state),
    start,
    stop
  }
}
