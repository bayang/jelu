<script setup lang="ts">
import { Ref, ref, watch } from "vue";
import dataService from "../services/DataService";
import { useI18n } from 'vue-i18n'
import { Tag } from "../model/Tag";
import { ObjectUtils } from "../utils/ObjectUtils";
import { useOruga } from "@oruga-ui/oruga-next";

const oruga = useOruga()

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

const props = defineProps<{
  ids: Array<string>,
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const toRead: Ref<boolean|null> = ref(null)
const owned: Ref<boolean|null> = ref(null)
const add: Ref<boolean> = ref(true)

let filteredTags: Ref<Array<Tag>> = ref([]);
const isFetching = ref(false)

function getFilteredTags(text: string) {
  isFetching.value = true
  dataService.findTagsByCriteria(text).then((data) => filteredTags.value = data.content)
  isFetching.value = false
}

function manageTag(tag: Tag, event: UIEvent) {
  if (tag != null && event != null && tag.id != null) {
    console.log("received tag : " + tag.id + "/" + tag.name)
    if (add.value) {
      if (addTags.value.findIndex(elem => tag.id === elem.id) === -1) {
        addTags.value.push(tag)
      }
    } else {
      if (removeTags.value.findIndex(elem => tag.id === elem.id) === -1) {
        removeTags.value.push(tag)
      }
    }
  }
}

function removeTag(tag: Tag, tagList: Array<Tag>) {
  let idx = tagList.findIndex(elem => tag.id === elem.id);
  if (idx !== -1) {
    tagList.splice(idx, 1)
  }
}

watch(owned, (newVal, oldVal) => {
  console.log("owned " + owned.value)
})

watch(toRead, (newVal, oldVal) => {
  console.log("toRead " + toRead.value)
})

const addTags: Ref<Array<Tag>> = ref([])
const removeTags: Ref<Array<Tag>> = ref([])

const submit = () => {
  if (owned.value != null || toRead.value != null || removeTags.value.length > 0 || addTags.value.length > 0) {
    dataService.bulkEditUserBooks({
      ids: props.ids,
      owned: owned.value != null ? owned.value : undefined,
      toRead: toRead.value != null ? toRead.value : undefined,
      addTags: addTags.value.map(tag => tag.id) as string[],
      removeTags: removeTags.value.map(tag => tag.id) as string[]
    }).then(res => {
      emit('close')
    }).catch(err => {
      console.log("error " + err)
      ObjectUtils.toast(oruga, "danger", t('labels.error_message', {msg : err.message}), 4000)
    })
  }
}

</script>

<template>
  <section class="event-modal">
    <div>
      <div>
        <div>
          <h1 class="typewriter text-2xl first-letter:capitalize">
            {{ t('bulk.bulk_edit_books') }}
          </h1>
        </div>
      </div>
      <div>
        <div class="field">
          <label class="label">
            <span class="label-text font-semibold">{{ t('bulk.owned') }} :</span>
          </label>
          
          <div class="flex gap-1">
            <label>{{ t('labels.yes') }}</label>
            <input
              v-model="owned"
              type="radio"
              :value="true"
              class="radio radio-accent"
            >
            <label>{{ t('labels.no') }}</label>
            <input
              v-model="owned"
              type="radio"
              :value="false"
              class="radio radio-accent"
            >
            <label>{{ t('labels.do_nothing') }}</label>
            <input
              v-model="owned"
              type="radio"
              :value="null"
              class="radio radio-accent"
            >
          </div>
        </div>
        <div class="field">
          <label class="label">
            <span class="label-text font-semibold first-letter:capitalize">{{ t('bulk.toRead') }} :</span>
          </label>
          <div class="flex gap-1">
            <label>{{ t('labels.yes') }}</label>
            <input
              v-model="toRead"
              type="radio"
              :value="true"
              class="radio radio-accent"
            >
            <label>{{ t('labels.no') }}</label>
            <input
              v-model="toRead"
              type="radio"
              :value="false"
              class="radio radio-accent"
            >
            <label>{{ t('labels.do_nothing') }}</label>
            <input
              v-model="toRead"
              type="radio"
              :value="null"
              class="radio radio-accent"
            >
          </div>
        </div>
        <div class="field">
          <label class="label">
            <span
              class="label-text font-semibold first-letter:capitalize"
            >{{ t('bulk.add_or_remove') }} :</span>
          </label>
          <input
            v-model="add"
            type="checkbox"
            class="toggle toggle-primary"
          >
          <span class="mx-2">{{ add == true ? "Add tag" : "Remove tag" }}</span>
        </div>
        <div class="field">
          <o-field :label="t('bulk.choose_tag')">
            <o-autocomplete
              :data="filteredTags"
              :clear-on-select="true"
              field="name"
              :loading="isFetching"
              :debounce="100"
              @input="getFilteredTags"
              @select="manageTag"
            />
          </o-field>
        </div>
        <div
          v-if="addTags.length > 0"
          class="field"
        >
          <span>{{ t('bulk.add') }}</span>
          <br>
          <span
            v-for="tag in addTags"
            :key="tag.id"
            class="badge badge-info"
          >
            {{ tag.name }}
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              class="inline-block w-4 h-4 stroke-current hover:cursor-pointer"
              @click="removeTag(tag, addTags)"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </span>
        </div>
        <div
          v-if="removeTags.length > 0"
          class="field"
        >
          <span>{{ t('bulk.remove') }}</span>
          <br>
          <span
            v-for="tag in removeTags"
            :key="tag.id"
            class="badge badge-warning"
          >
            {{ tag.name }}
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              class="inline-block w-4 h-4 stroke-current hover:cursor-pointer"
              @click="removeTag(tag, removeTags)"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </span>
        </div>
        <div class="my-3">
          <button
            class="btn btn-secondary mr-2 uppercase"
            @click="submit"
          >
            <span class="icon">
              <i class="mdi mdi-pencil mdi-18px" />
            </span>
            <span>{{ t('labels.submit') }}</span>
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<style lang="scss">
</style>
