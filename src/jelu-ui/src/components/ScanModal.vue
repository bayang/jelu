<script setup lang="ts">
import { ref } from "vue";
import { StreamBarcodeReader } from "vue-barcode-reader";
import { useI18n } from 'vue-i18n';

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const emit = defineEmits<{
  (e: 'close'): void,
  (e: 'decoded', barcode: string|null): void
}>()

const decodedText = ref("");

const acceptBarcode = () => {
    emit('decoded', decodedText.value)
    emit('close')
}

const onLoaded = () => {
  console.log("barcode modal loaded");
};
const onDecode = (text: string) => {
  console.log("barcode " + text);
  decodedText.value = text;
  acceptBarcode()
};

</script>

<template>
  <section class="edit-modal">
    <div class="grid justify-center justify-items-center columns is-centered is-multiline">
      <div class="mb-2">
        <h1 class="text-2xl title has-text-weight-normal typewriter capitalize">
          {{ t('labels.import_book') }}
        </h1>
      </div>
      <div
        class="column is-centered is-full"
      >
        <div class="field mb-2">
          <StreamBarcodeReader
            @decode="onDecode"
            @loaded="onLoaded"
          />
          <p>{{ decodedText }}</p>
        </div>
      </div>
    </div>
  </section>
</template>

<style lang="scss">

</style>
