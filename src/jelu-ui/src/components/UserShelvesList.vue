<script setup lang="ts">
import { computed, Ref, ref, watch } from "vue";
import { useI18n } from 'vue-i18n';
import usePagination from "../composables/pagination";
import { Shelf } from "../model/Shelf";
import dataService from "../services/DataService";
import { ObjectUtils } from "../utils/ObjectUtils";
import { Tag } from "../model/Tag";
import { useOruga } from "@oruga-ui/oruga-next";
import { useTitle } from "@vueuse/core";
import useTypography from "../composables/typography";
import { ArrowDownAZ, ArrowDownZA, CalendarArrowDown, CalendarArrowUp, Trash } from '@lucide/vue';

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })
useTitle('Jelu | User page')

const props = defineProps<{
  allowDelete: boolean,
  allowCreate: boolean
}>()
const oruga = useOruga()

const { total, page, pageAsNumber, perPage, updatePage, getPageIsLoading, updatePageLoading, pageCount } = usePagination(12, false)

const sortQuery = ref('name,asc')
const filteredTags: Ref<Array<Tag>> = ref([]);
const isFetching = ref(false)

function getFilteredTags(text: string) {
  isFetching.value = true
  dataService.findTagsByCriteria(text).then((data) => filteredTags.value = data.content)
  isFetching.value = false
}

const shelves: Ref<Array<Shelf>> = ref([])

const emit = defineEmits<{
  (e: 'close'): void,
  (e: 'shelves-changed'): void
}>()

const getShelves = () => {
  getPageIsLoading.value = true
  dataService.shelves(undefined, undefined,
  pageAsNumber.value - 1, perPage.value, sortQuery.value)
  .then(res => {
        console.log(res)
          total.value = res.totalElements
          shelves.value = res.content
        if (! res.empty) {
          page.value =  (res.number + 1).toString(10)
        }
        else {
          page.value = "1"
        }
        getPageIsLoading.value = false
        updatePageLoading(false)
    }
    )
    .catch(e => {
      getPageIsLoading.value = false
      updatePageLoading(false)
    })
}

function deleteShelf(shelf: Shelf) {
  dataService.deleteShelf(shelf.id)
  .then(res => {
    console.log("deleted shelf " + shelf.id)
    emit('shelves-changed')
    getShelves()
    })
  .catch(err => console.log("failed to delete shelf " + shelf.id))
}

const options = computed(() => {
  return filteredTags.value.map(t => ObjectUtils.wrapForOptions(t))
})

function createShelfFromTag(tag: Tag) {
  console.log(tag)
  // we receive from oruga weird events while nothing is selected
  // so try to get rid of those null data we receive
  if (tag != null && tag.id != null) {
    dataService.saveShelf({name: tag.name, targetId: tag.id ?? ""})
      .then(res => {
        console.log("saved shelf " + res.name)
        // store.dispatch('getUserShelves')
        filteredTags.value = []
        getShelves()
      })
      .catch(err => {
        console.log("failed to save shelf " + tag.name + " " + err)
        ObjectUtils.toast(oruga, "danger", t('labels.error_message'), 4000)
    })
  }
}

watch([page, sortQuery], (newVal, oldVal) => {
  console.log("all " + newVal + " " + oldVal)
  if (newVal !== oldVal) {
    getShelves()
  }
})

getShelves()

const { typographyClasses } = useTypography()
</script>

<template>
  <div class="w-fit flex flex-col sm:flex-row-reverse">
    <div
      v-if="props.allowCreate"
      class="sm:mx-1 w-full sm:w-fit"
    >
      <div class="field">
        <p>{{ t('settings.shelf_choose_tag') }}</p>
        <o-autocomplete
          :input-classes="{rootClass:'border-2 border-accent w-full'}"
          :loading="isFetching"
          class="w-full"
          open-on-focus
          backend-filtering
          clear-on-select
          :debounce="100"
          :options="options"
          @input="getFilteredTags"
          @select="createShelfFromTag"
        >
          <template #default="{ value }">
            <div class="jl-taginput-item">
              {{ value.name }}
            </div>
          </template>
        </o-autocomplete>
      </div>
    </div>

    <div>
      <div>
        <h1
          class="text-2xl first-letter:capitalize"
          :class="typographyClasses"
        >
          {{ t('settings.shelves') }}
        </h1>
      </div>
      <div class="flex flex-col mb-2">
        <div class="flex my-2 gap-2">
          <arrow-down-a-z
            class="cursor-pointer"
            @click="sortQuery='name,asc'"
          />
          <arrow-down-z-a
            class="cursor-pointer"
            @click="sortQuery='name,desc'"
          />
          <calendar-arrow-down
            class="cursor-pointer"
            @click="sortQuery='modificationDate,asc'"
          />
          <calendar-arrow-up
            class="cursor-pointer"
            @click="sortQuery='modificationDate,desc'"
          />
        </div>
        <div
          v-for="shelf in shelves"
          :key="shelf.id"
          class="my-0.5"
        >
          <div class="alert shadow-lg w-full jl-card">
            <i class="mdi mdi-bookshelf mdi-24px" />
            <h3 class="font-bold">
              <router-link
                class="link"
                :to="{ name: 'tag-detail', params: { tagId: shelf.targetId }, query: {sort: 'modificationDate,desc'} }"
                @click="emit('close')"
              >
                {{ shelf.name }}
              </router-link>
            </h3>
            <button
              v-if="props.allowDelete"
              class="btn btn-sm"
              @click="deleteShelf(shelf)"
            >
              <trash :size="21" />
            </button>
          </div>
        </div>
        <o-pagination
          v-if="shelves.length > 0"
          :current="pageAsNumber"
          :total="total"
          order="centered"
          :per-page="perPage"
          @change="updatePage"
        />
        <progress
          v-if="getPageIsLoading"
          class="animate-pulse progress progress-success mt-5"
          max="100"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
  .o-dropdown.o-dropdown--position-auto.o-autocomplete,
  .o-dropdown.o-dropdown--position-bottom.o-autocomplete {
    @apply w-full;
  }
</style>
