<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useI18n } from 'vue-i18n';
import { DetectedBarcode, EmittedError, QrcodeStream } from 'vue-qrcode-reader';

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

const torchActive = ref(false)
const torchNotSupported = ref(false)

const acceptBarcode = () => {
    emit('decoded', decodedText.value)
    emit('close')
}

// eslint-disable-next-line no-undef
const onLoaded = (capabilities: Partial<MediaTrackCapabilities>) => {
  console.log("barcode modal loaded");
  console.log(capabilities)
  torchNotSupported.value = !(capabilities as any).torch
  loading.value = false
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
}

onMounted(async () => {
  devices.value = (await navigator.mediaDevices.enumerateDevices()).filter(
    ({ kind }) => kind === 'videoinput'
  )

  if (devices.value.length > 0) {
    selected.value = devices.value[0]
  }
})
</script>

<template>
  <section class="edit-modal">
    <div class="grid justify-center justify-items-center">
      <div class="mb-2">
        <h1 class="text-2xl typewriter capitalize">
          {{ t('labels.import_book') }}
        </h1>
      </div>
      <div>
        <div class="field mb-2">
          <p>
            {{ t('labels.pick_camera') }}:
            <select v-model="selected">
              <option
                v-for="device in devices"
                :key="device.label"
                :value="device"
              >
                {{ device.label }}
              </option>
            </select>
          </p>
          <qrcode-stream
            ref="barcodeReader"
            v-memo="[torchActive, selected?.deviceId]"
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
