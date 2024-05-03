<script setup lang="ts">
import { Ref, ref } from "vue";
import { useI18n } from 'vue-i18n';
import draggable from "vuedraggable";
import { useStore } from 'vuex';
import { PluginInfo, PluginInfoOrder } from "../model/PluginInfo";
import { key } from '../store';

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

const store = useStore(key)

const emit = defineEmits<{
  (e: 'close'): void,
  (e: 'plugins', plugins: Array<PluginInfo>): void
}>()

const plugins: Ref<Array<PluginInfoOrder>> = ref([])
for (const elem of store.getters.getSettings.metadataPlugins) {
  console.log('elem' + elem)
  plugins.value.push({name: elem.name, order: elem.order, enabled: true})

}
plugins.value.sort((a, b) => b.order - a.order)
const progress: Ref<boolean> = ref(false)

 const submit =async () => {
  console.log("plugins")
  console.log(plugins.value)
  let finalPlugins = plugins.value.filter(p => p.enabled).map((pluginInfo, idx) => newPlugin(pluginInfo, idx, plugins.value.length))
  console.log("new plugins")
  console.log(finalPlugins)
  emit('plugins', finalPlugins)
  emit('close')
}

const newPlugin = (plugin: PluginInfo, idx: number, size: number): PluginInfo => {
  return {name: plugin.name, order: size - idx}
}

const dismiss =async () => {
  emit('close')
}

function getKey(item: PluginInfo) {
      return item.order;
}

/** move item to the bottom if disabled */
const checkState = (element: PluginInfoOrder) => {
  console.log("state")
  console.log(element)
  if (element.enabled === false) {
    plugins.value = [...plugins.value.filter(a => a !== element), element]
  } else {
    plugins.value = [element, ...plugins.value.filter(a => a !== element)]
  }
}

/** prevent disabled items to be dragged */
function checkMove(evt: any){
    return (evt.draggedContext.element.enabled);
}

</script>

<template>
  <section class="metadata-modal">
    <div class="flex flex-col items-center justify-items-center">
      <h1
        class="typewriter text-2xl first-letter:capitalize"
      >
        {{ t('metadata.reorder_plugins') }}
      </h1>
      <p>{{ t('metadata.description') }}</p>
      <div class="flex flex-wrap place-content-center my-3">
        <draggable
          v-model="plugins"
          class="list-group"
          :item-key="getKey"
          :move="checkMove"
          tag="div"
        >
          <template #item="{ element }">
            <div
              :class="element.enabled ? 'hover:cursor-move border-accent' : 'border-base-content/30 hover:cursor-not-allowed text-base-content/30'"
              class="flex flex-row justify-between items-center border-2 m-1 p-1"
            >
              <span class="mdi mdi-drag-vertical mdi-24 text-3xl" />
              <div class="flex flex-col">
                <span 
                  class="text font-semibold"
                >{{ element.name }}</span>
                <span class="text text-xs">({{ t('metadata.default_priority') }}:{{ element.order }}) </span>
              </div>
              <div class="justify-self-end">
                <input
                  v-model="element.enabled"
                  type="checkbox"
                  class="toggle toggle-accent"
                  @change="checkState(element)"
                >
              </div>
            </div>
          </template>
        </draggable>
        <div class="my-3">
          <button
            class="btn btn-primary mr-2 uppercase"
            :disabled="progress"
            @click="submit"
          >
            <span
              v-if="progress"
              class="loading loading-spinner"
            />
            <span class="icon">
              <i class="mdi mdi-pencil mdi-18px" />
            </span>
            <span>{{ t('labels.submit') }}</span>
          </button>
          <button
            class="btn btn-secondary mr-2 uppercase"
            :disabled="progress"
            @click="dismiss"
          >
            <span
              v-if="progress"
              class="loading loading-spinner"
            />
            <span class="icon">
              <i class="mdi mdi-cancel mdi-18px" />
            </span>
            <span>{{ t('labels.discard') }}</span>
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<style lang="scss">
</style>
