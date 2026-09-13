<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useI18n } from 'vue-i18n';
import { DetectedBarcode, EmittedError, QrcodeStream } from 'vue-qrcode-reader';
import useTypography from "../composables/typography";

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const emit = defineEmits<{
  (e: 'close'): void,
  (e: 'decoded', barcode: string|null): void,
  (e: 'barcodeLoaded', reader: any): void
}>()

const decodedText = ref("");
const barcodeReader = ref()
const loading = ref(true)

const selected = ref(null as MediaDeviceInfo | null)
const devices = ref([] as MediaDeviceInfo[])
const devicesReady = ref(false)

const torchActive = ref(false)
const torchNotSupported = ref(false)

// remembers the user's camera choice across modal openings
const STORAGE_KEY = 'jelu.scanner.deviceId'

// upstream default (facingMode: environment) unless the user picked a device
const constraints = computed(() => {
  if (selected.value?.deviceId) {
    return {
      deviceId: { exact: selected.value.deviceId },
      width: { ideal: 1920 },
      height: { ideal: 1080 }
    }
  }
  return {
    facingMode: 'environment',
    width: { ideal: 1920 },
    height: { ideal: 1080 }
  }
})

// draws a frame around detected codes
const paintOutline = (detectedCodes: Array<DetectedBarcode>, ctx: CanvasRenderingContext2D) => {
  for (const detectedCode of detectedCodes) {
    const [firstPoint, ...otherPoints] = detectedCode.cornerPoints
    if (!firstPoint) continue

    ctx.strokeStyle = "#22cc55"
    ctx.lineWidth = 4

    ctx.beginPath()
    ctx.moveTo(firstPoint.x, firstPoint.y)
    for (const { x, y } of otherPoints) {
      ctx.lineTo(x, y)
    }
    ctx.lineTo(firstPoint.x, firstPoint.y)
    ctx.closePath()
    ctx.stroke()
  }
}

const acceptBarcode = () => {
    emit('decoded', decodedText.value)
    emit('close')
}

const loadDevices = async () => {
  devices.value = (await navigator.mediaDevices.enumerateDevices()).filter(
    ({ kind }) => kind === 'videoinput'
  )
}

// apply the saved camera once labels are available; drop a stale saved id
const applyStoredDevice = () => {
  if (devices.value.length === 0 || devices.value.some((d) => !d.label)) return
  let storedId = null as string | null
  try {
    storedId = localStorage.getItem(STORAGE_KEY)
  } catch (e) {
    console.log(e)
  }
  if (!storedId) return
  const match = devices.value.find((d) => d.deviceId === storedId)
  if (match) {
    selected.value = match
  } else {
    try {
      localStorage.removeItem(STORAGE_KEY)
    } catch (e) {
      console.log(e)
    }
  }
}

// save the user's camera pick; never write from script
const onDeviceChange = () => {
  if (!selected.value?.deviceId) return
  try {
    localStorage.setItem(STORAGE_KEY, selected.value.deviceId)
  } catch (e) {
    console.log(e)
  }
}

// eslint-disable-next-line no-undef
const onLoaded = async (capabilities: Partial<MediaTrackCapabilities>) => {
  console.log("barcode modal loaded");
  console.log(capabilities)
  torchNotSupported.value = !(capabilities as any).torch
  loading.value = false
  if (devices.value.length === 0 || devices.value.some((d) => !d.label)) {
    try {
      // labels are empty before permission is granted; refresh now that it is
      await loadDevices()
    } catch (error) {
      console.log(error)
    }
    if (selected.value && !devices.value.find((d) => d.deviceId === selected.value?.deviceId)) {
      selected.value = null
    }
  }
  emit('barcodeLoaded', barcodeReader.value)
};

const onDecode = (detectedBarcodes: Array<DetectedBarcode>) => {
  console.log("barcode ");
  console.log(detectedBarcodes)
  decodedText.value = detectedBarcodes[0].rawValue
  acceptBarcode()
};

const onError = (error: EmittedError) => {
  console.log("barcode reader error")
  console.log(error)
  // avoid a permanent lock on a bad saved id
  try {
    localStorage.removeItem(STORAGE_KEY)
  } catch (e) {
    console.log(e)
  }
}

onMounted(async () => {
  try {
    await loadDevices()
  } finally {
    devicesReady.value = true
  }
  applyStoredDevice()
})

const { typographyClasses } = useTypography()
</script>

<template>
  <section class="edit-modal">
    <div class="grid justify-center justify-items-center">
      <div class="mb-2">
        <h1
          class="text-2xl capitalize"
          :class="typographyClasses"
        >
          {{ t('labels.import_book') }}
        </h1>
      </div>
      <div>
        <div class="field mb-2">
          <p>
            {{ t('labels.pick_camera') }}:
            <select
              v-model="selected"
              @change="onDeviceChange"
            >
              <option
                v-for="(device, index) in devices"
                :key="device.deviceId || index"
                :value="device"
              >
                {{ device.label }}
              </option>
            </select>
          </p>
          <qrcode-stream
            v-if="devicesReady"
            ref="barcodeReader"
            :constraints="constraints"
            :torch="torchActive"
            :track="paintOutline"
            :formats="['qr_code', 'ean_13']"
            @detect="onDecode"
            @camera-on="onLoaded"
            @error="onError"
          >
            <div
              v-if="loading"
              class="loading-indicator"
            >
              {{ t('labels.loading') }}...
            </div>
            <button
              v-else
              :disabled="torchNotSupported"
              @click="torchActive = !torchActive"
            >
              <svg
                v-if="torchActive"
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                stroke-width="1.5"
                stroke="currentColor"
                class="size-6"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="m3.75 13.5 10.5-11.25L12 10.5h8.25L9.75 21.75 12 13.5H3.75Z"
                />
              </svg>
              <svg
                v-else
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                stroke-width="1.5"
                stroke="currentColor"
                class="size-6"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M11.412 15.655 9.75 21.75l3.745-4.012M9.257 13.5H3.75l2.659-2.849m2.048-2.194L14.25 2.25 12 10.5h8.25l-4.707 5.043M8.457 8.457 3 3m5.457 5.457 7.086 7.086m0 0L21 21"
                />
              </svg>
            </button>
          </qrcode-stream>
          <div
            v-else
            class="loading-indicator"
          >
            {{ t('labels.loading') }}...
          </div>
          <p>{{ decodedText }}</p>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.loading-indicator {
  font-weight: bold;
  font-size: 2rem;
  text-align: center;
}
</style>
