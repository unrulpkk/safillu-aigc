<template>
  <doc-alert :title="t('native.nb4cf2dbc22')" url="https://doc.iocoder.cn/redis-cache/" />
  <doc-alert :title="t('native.na6bf6336ce')" url="https://doc.iocoder.cn/local-cache/" />

  <el-scrollbar height="calc(100vh - 88px - 40px - 50px)">
    <el-row>
      <!-- 基本信息 -->
      <el-col :span="24" class="card-box" shadow="hover">
        <el-card>
          <el-descriptions :title="t('native.ne8df058725')" :column="descriptionColumns" border>
            <el-descriptions-item :label="t('native.n1081575124')">
              {{ cache?.info?.redis_version }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('native.nd87f1ca761')">
              {{
                cache?.info?.redis_mode == 'standalone'
                  ? t('modules.na86b9e4056')
                  : t('modules.n5318ca46e3')
              }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('native.n2b174585f5')">
              {{ cache?.info?.tcp_port }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('native.n206b3399b8')">
              {{ cache?.info?.connected_clients }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('native.n190e761c96')">
              {{ cache?.info?.uptime_in_days }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('native.n83841bc756')">
              {{ cache?.info?.used_memory_human }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('native.na4e2c15c0c')">
              {{ cache?.info ? parseFloat(cache?.info?.used_cpu_user_children).toFixed(2) : '' }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('native.n9c2a4f0eb0')">
              {{ cache?.info?.maxmemory_human }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('native.n42bd3e9b82')">
              {{
                cache?.info?.aof_enabled == '0' ? t('native.n0c70665b6e') : t('native.nb5141d3d19')
              }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('native.nd1090b7e36')">
              {{ cache?.info?.rdb_last_bgsave_status }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('native.nf88c863c69')">
              {{ cache?.dbSize }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('native.n58c0af68ec')">
              {{ cache?.info?.instantaneous_input_kbps }}kps/
              {{ cache?.info?.instantaneous_output_kbps }}kps
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <!-- 命令统计 -->
      <el-col :span="12" :xs="24" class="mt-3">
        <el-card :gutter="12" shadow="hover">
          <Echart :options="commandStatsRefChika" :height="420" />
        </el-card>
      </el-col>
      <!-- 内存使用量统计 -->
      <el-col :span="12" :xs="24" class="mt-3">
        <el-card class="ml-3" :gutter="12" shadow="hover">
          <Echart :options="usedmemoryEchartChika" :height="420" />
        </el-card>
      </el-col>
    </el-row>
  </el-scrollbar>
</template>
<script lang="ts" setup>
import { useWindowSize } from '@vueuse/core'
const { width: viewportWidth } = useWindowSize()
const descriptionColumns = computed(() =>
  viewportWidth.value < 640 ? 1 : viewportWidth.value < 1100 ? 2 : 3
)

const { t } = useI18n()

import { useLocaleStore } from '@/store/modules/locale'
import * as RedisApi from '@/api/infra/redis'
import { RedisMonitorInfoVO } from '@/api/infra/redis/types'
import type { EChartsOption } from 'echarts'

const cache = ref<RedisMonitorInfoVO>()

// 基本信息
const readRedisInfo = async () => {
  cache.value = await RedisApi.getCache()
}

// 内存使用情况
const usedmemoryEchartChika = reactive<EChartsOption>({
  title: {
    // 仪表盘标题。
    text: t('native.na816161040'),
    left: 'center',
    show: true, // 是否显示标题,默认 true。
    textStyle: {
      color: '#374151', // 文字的颜色,默认 #333。
      fontSize: 20 // 文字的字体大小,默认 15。
    }
  },
  toolbox: {
    show: false,
    feature: {
      restore: { show: true },
      saveAsImage: { show: true }
    }
  },
  series: [
    {
      name: t('native.n5fa34377dd'),
      type: 'gauge',
      min: 0,
      max: 50,
      splitNumber: 10,
      //这是指针的颜色
      color: '#F5C74E',
      radius: '85%',
      center: ['50%', '50%'],
      startAngle: 225,
      endAngle: -45,
      axisLine: {
        // 坐标轴线
        lineStyle: {
          // 属性lineStyle控制线条样式
          color: [
            [0.2, '#7FFF00'],
            [0.8, '#00FFFF'],
            [1, '#FF0000']
          ],
          //width: 6 外框的大小（环的宽度）
          width: 10
        }
      },
      axisTick: {
        // 坐标轴小标记
        //里面的线长是5（短线）
        length: 5, // 属性length控制线长
        lineStyle: {
          // 属性lineStyle控制线条样式
          color: '#76D9D7'
        }
      },
      splitLine: {
        // 分隔线
        length: 20, // 属性length控制线长
        lineStyle: {
          // 属性lineStyle（详见lineStyle）控制线条样式
          color: '#76D9D7'
        }
      },
      axisLabel: {
        color: '#76D9D7',
        distance: 15,
        fontSize: 15
      },
      pointer: {
        // 指针的大小
        width: 7,
        show: true
      },
      detail: {
        fontWeight: 'normal',
        // 里面文字下的数值大小（50）
        fontSize: 15,
        color: '#FFFFFF',
        valueAnimation: true
      },
      progress: {
        show: true
      }
    }
  ]
}) as EChartsOption

// 指令使用情况
const commandStatsRefChika = reactive<EChartsOption>({
  title: {
    text: t('native.n9b5b92ead2'),
    left: 'center'
  },
  tooltip: {
    trigger: 'item',
    formatter: '{a} <br/>{b} : {c} ({d}%)'
  },
  legend: {
    type: 'scroll',
    orient: 'vertical',
    right: 30,
    top: 10,
    bottom: 20,
    data: [] as any[],
    textStyle: {
      color: '#a1a1a1'
    }
  },
  series: [
    {
      name: t('native.n928f87d450'),
      type: 'pie',
      radius: [20, 120],
      center: ['40%', '60%'],
      data: [] as any[],
      roseType: 'radius',
      label: {
        show: true
      },
      emphasis: {
        label: {
          show: true
        },
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      }
    }
  ]
}) as EChartsOption

const localeStore = useLocaleStore()
watch(
  () => localeStore.getCurrentLocale.lang,
  () => {
    ;(commandStatsRefChika.title as any).text = t('native.n9b5b92ead2')
    ;(usedmemoryEchartChika.title as any).text = t('native.na816161040')
  }
)

/** 加载数据 */
const getSummary = () => {
  // 初始化命令图表
  initCommandStatsChart()
  usedMemoryInstance()
}

/** 命令使用情况 */
const initCommandStatsChart = async () => {
  const usedMemorySeries = usedmemoryEchartChika.series![0] as any
  usedMemorySeries.data = []
  // 发起请求
  try {
    const data = await RedisApi.getCache()
    cache.value = data
    // 处理数据
    const commandStats = [] as any[]
    const nameList = [] as string[]
    data.commandStats.forEach((row) => {
      commandStats.push({
        name: row.command,
        value: row.calls
      })
      nameList.push(row.command)
    })
    ;(commandStatsRefChika.legend as any).data = nameList
    ;(commandStatsRefChika.series![0] as any).data = commandStats
  } catch {}
}
const usedMemoryInstance = async () => {
  try {
    const data = await RedisApi.getCache()
    cache.value = data
    // 仪表盘详情，用于显示数据。
    const usedMemorySeries = usedmemoryEchartChika.series![0] as any
    usedMemorySeries.detail = {
      show: true, // 是否显示详情,默认 true。
      offsetCenter: [0, '50%'], // 相对于仪表盘中心的偏移位置，数组第一项是水平方向的偏移，第二项是垂直方向的偏移。可以是绝对的数值，也可以是相对于仪表盘半径的百分比。
      color: 'auto', // 文字的颜色,默认 auto。
      fontSize: 30, // 文字的字体大小,默认 15。
      formatter: cache.value!.info.used_memory_human // 格式化函数或者字符串
    }

    usedMemorySeries.data[0] = {
      value: cache.value!.info.used_memory_human,
      name: t('native.nbf260ab3e1')
    }
    console.log(cache.value!.info)
    ;(usedmemoryEchartChika as any).tooltip = {
      formatter: '{b} <br/>{a} : ' + cache.value!.info.used_memory_human
    }
  } catch {}
}

/** 初始化 **/
onMounted(() => {
  // 读取 redis 信息
  readRedisInfo()
  // 加载数据
  getSummary()
})
</script>
