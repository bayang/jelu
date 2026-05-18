<script setup lang="ts">
import { useThrottleFn, useTitle } from '@vueuse/core';
import { useRouteQuery } from '@vueuse/router';
import { computed, Ref, ref, watch, onMounted, nextTick } from 'vue'; // added nextTick
import { useI18n } from 'vue-i18n';
import usePagination from '../composables/pagination';
import useSort from '../composables/sort';
import useBulkEdition from '../composables/bulkEdition';
import { UserBook } from '../model/Book';
import { ReadingEventType } from '../model/ReadingEvent';
import dataService from "../services/DataService";
import BookCard from "./BookCard.vue";
import SortFilterBarVue from "./SortFilterBar.vue";
import useTypography from '../composables/typography';
import Sortable from 'sortablejs'; // NEW: import SortableJS

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

useTitle('Jelu | ' + t('nav.to_read'))

const books: Ref<Array<UserBook>> = ref([]);

const { total, page, pageAsNumber, perPage, updatePage, getPageIsLoading, updatePageLoading, pageCount } = usePagination()

const { sortQuery, sortOrder, sortBy, sortOrderUpdated } = useSort('creationDate,desc')

const { showSelect, selectAll, checkedCards, cardChecked, toggleEdit } = useBulkEdition(modalClosed)

const eventTypes: Ref<Array<ReadingEventType>> = useRouteQuery('lastEventTypes', [])

const userId: Ref<string|null> = useRouteQuery('userId', null)
const username = ref("")

const getUsername = async () => {
  if (userId.value != null) {
    username.value = await dataService.usernameById(userId.value)
  }
}

getUsername()

const open = ref(false)

const owned: Ref<string|null> = useRouteQuery('owned', "null")
const ownedAsBool = computed(() => {
  if (owned.value?.toLowerCase() === "null") {
    return null
  } else if (owned.value?.toLowerCase() === "true") {
    return true
  } else {
    return false
  }
})

const getToReadIsLoading: Ref<boolean> = ref(false)

// NEW: custom sort mode state
const customSortMode = ref(false)
const isSavingSortOrder = ref(false)
const gridRef = ref<HTMLElement | null>(null) // NEW: ref to the grid DOM element
let sortableInstance: Sortable | null = null  // NEW: holds the SortableJS instance

const getToRead = async () => {
  getToReadIsLoading.value = true
  try {
    // NEW: in custom sort mode, fetch all books unpaginated and sort by sortOrder
    const pageSize = customSortMode.value ? 10000 : perPage.value
    const sortParam = customSortMode.value ? 'sortOrder,asc' : sortQuery.value

    const res = await dataService.findUserBookByCriteria(
      eventTypes.value, null, userId.value,
      true, ownedAsBool.value, null,
      0, pageSize, sortParam)
    total.value = res.totalElements
    books.value = res.content
    if (!res.empty) {
      page.value = (res.number + 1).toString(10)
    } else {
      page.value = "1"
    }
    getToReadIsLoading.value = false
    updatePageLoading(false)
    removeIds()

    // NEW: after books load in custom sort mode, initialise SortableJS on the grid
    if (customSortMode.value) {
      await nextTick() // wait for Vue to render the updated book list
      initSortable()
    }
  } catch (error) {
    console.log("failed get books : " + error);
    getToReadIsLoading.value = false
    updatePageLoading(false)
  }
};

const removeIds = () => {
  if (userId.value != null) {
    books.value.forEach(b => b.id = undefined)
  }
}

// NEW: initialise SortableJS on the grid element
const initSortable = () => {
  if (sortableInstance) {
    sortableInstance.destroy() // destroy any previous instance before creating a new one
    sortableInstance = null
  }
  if (gridRef.value) {
    sortableInstance = Sortable.create(gridRef.value, {
      animation: 150,           // smooth animation when dragging
      ghostClass: 'opacity-30', // tailwind class to dim the dragged item's placeholder
      onEnd(evt) {
        // when drag ends, reorder the books array to match the new DOM order
        if (evt.oldIndex !== undefined && evt.newIndex !== undefined) {
          const moved = books.value.splice(evt.oldIndex, 1)[0]
          books.value.splice(evt.newIndex, 0, moved)
        }
      }
    })
  }
}

// NEW: destroy SortableJS when leaving custom sort mode
const destroySortable = () => {
  if (sortableInstance) {
    sortableInstance.destroy()
    sortableInstance = null
  }
}

// NEW: toggle custom sort mode on/off
const toggleCustomSort = async () => {
  customSortMode.value = !customSortMode.value
  if (customSortMode.value) {
    // entering custom sort mode: fetch all books sorted by saved sortOrder
    await getToRead()
  } else {
    // leaving without saving: destroy sortable and reload normal paginated view
    destroySortable()
    await getToRead()
  }
}

//save the current order to the backend
const saveCustomSortOrder = async () => {
  isSavingSortOrder.value = true
  try {
    const sortOrders: Record<string, number> = {}
    books.value.forEach((book, index) => {
      if (book.id) {
        sortOrders[book.id] = index
      }
    })
    await dataService.updateUserBooksSortOrder(sortOrders)
    customSortMode.value = false
    destroySortable()
    await getToRead()
  } catch (error) {
    console.log("failed to save sort order: " + error)
  } finally {
    isSavingSortOrder.value = false
  }
}

const SORT_BY_KEY = "toReadSortBy";
const SORT_ORDER_KEY = "toReadSortOrder";
const EVENT_TYPES_KEY = "toReadEventTypes";
const OWNED_KEY = "toReadOwned";

onMounted(() => {
  const savedSortBy = localStorage.getItem(SORT_BY_KEY);
  const savedSortOrder = localStorage.getItem(SORT_ORDER_KEY);
  if (savedSortBy) sortBy.value = savedSortBy;
  if (savedSortOrder) sortOrder.value = savedSortOrder;

  const savedEventTypes = localStorage.getItem(EVENT_TYPES_KEY);
  if (savedEventTypes) {
    try {
      eventTypes.value = JSON.parse(savedEventTypes);
    } catch {}
  }

  const savedOwned = localStorage.getItem(OWNED_KEY);
  if (savedOwned) owned.value = savedOwned;
});

watch(sortBy, (newVal) => {
  localStorage.setItem(SORT_BY_KEY, newVal);
});
watch(sortOrder, (newVal) => {
  localStorage.setItem(SORT_ORDER_KEY, newVal);
});
watch(eventTypes, (newVal) => {
  localStorage.setItem(EVENT_TYPES_KEY, JSON.stringify(newVal));
}, { deep: true });
watch(owned, (newVal) => {
  localStorage.setItem(OWNED_KEY, newVal ?? "null");
});

const throttledGetToRead = useThrottleFn(() => {
  getToRead()
}, 100, false)

watch([page, eventTypes, sortQuery, owned], (newVal, oldVal) => {
  if (customSortMode.value) return //ignore filter/sort changes during custom sort
  console.log("all " + newVal + " " + oldVal)
  if (newVal !== oldVal) {
    throttledGetToRead()
  }
})

function modalClosed() {
  console.log("modal closed")
  throttledGetToRead()
}

const message = computed(() => {
  if (userId.value != null) {
    return t('labels.reading_list_from_name', { name: username.value })
  } else {
    return t('nav.to_read')
  }
})

getToRead()

const { typographyClasses } = useTypography()
</script>

<template>
  <sort-filter-bar-vue
    :open="open"
    :order="sortOrder"
    @update:open="open = $event"
    @update:sort-order="sortOrderUpdated"
  >
    <template #sort-fields>
      <label class="label">{{ t('sorting.sort_by') }} : </label>
      <div class="field">
        <input v-model="sortBy" type="radio" name="radio-20" class="radio radio-primary my-2" value="creationDate">
        <span class="label-text">{{ t('sorting.date_added_to_list') }}</span>
      </div>
      <div class="field">
        <input v-model="sortBy" type="radio" name="radio-20" class="radio radio-primary my-2" value="lastReadingEventDate">
        <span class="label-text">{{ t('sorting.last_reading_event_date') }}</span>
      </div>
      <div class="field">
        <input v-model="sortBy" type="radio" name="radio-20" class="radio radio-primary mb-2" value="title">
        <span class="label-text">{{ t('sorting.title') }}</span>
      </div>
      <div class="field">
        <input v-model="sortBy" type="radio" name="radio-20" class="radio radio-primary mb-2" value="publisher">
        <span class="label-text">{{ t('sorting.publisher') }}</span>
      </div>
      <div class="field">
        <input v-model="sortBy" type="radio" name="radio-20" class="radio radio-primary mb-2" value="pageCount">
        <span class="label-text">{{ t('sorting.page_count') }}</span>
      </div>
      <div class="field">
        <input v-model="sortBy" type="radio" name="radio-20" class="radio radio-primary mb-2" value="usrAvgRating">
        <span class="label-text">{{ t('sorting.user_avg_rating') }}</span>
      </div>
      <div class="field">
        <input v-model="sortBy" type="radio" name="radio-20" class="radio radio-primary mb-2" value="avgRating">
        <span class="label-text">{{ t('sorting.avg_rating') }}</span>
        <div class="field">
          <input v-model="sortBy" type="radio" name="radio-20" class="radio radio-primary" value="random">
          <span class="label-text">{{ t('sorting.random') }}</span>
        </div>
      </div>
      <div class="field">
        <input v-model="sortBy" type="radio" name="radio-20" class="radio radio-primary mb-2" value="sortOrder">
        <span class="label-text">{{ t('sorting.custom_order') }}</span>
      </div>
    </template>
    <template #filters>
      <div class="field flex flex-col gap-1 capitalize">
        <label class="label">{{ t('reading_events.last_event_type') }} : </label>
        <label class="label">
          <input v-model="eventTypes" type="checkbox" class="checkbox checkbox-primary" value="FINISHED">
          {{ t('reading_events.finished') }}
        </label>
        <label class="label">
          <input v-model="eventTypes" type="checkbox" class="checkbox checkbox-primary" value="CURRENTLY_READING">
          {{ t('reading_events.currently_reading') }}
        </label>
        <label class="label">
          <input v-model="eventTypes" type="checkbox" class="checkbox checkbox-primary" value="DROPPED">
          {{ t('reading_events.dropped') }}
        </label>
      </div>
      <div class="field flex flex-col items-start">
        <label class="label">{{ t('filtering.owned') }} : </label>
        <div class="field">
          <input v-model="owned" type="radio" name="radio-31" class="radio radio-primary my-2" value="null">
          <span class="label-text">{{ t('filtering.unset') }}</span>
        </div>
        <div class="field">
          <input v-model="owned" type="radio" name="radio-31" class="radio radio-primary mb-2" value="false">
          <span class="label-text">{{ t('labels.false') }}</span>
        </div>
        <div class="field">
          <input v-model="owned" type="radio" name="radio-31" class="radio radio-primary" value="true">
          <span class="label-text">{{ t('labels.true') }}</span>
        </div>
      </div>
    </template>
  </sort-filter-bar-vue>

  <div class="flex flex-row justify-between mb-2">
    <div class="flex flex-row gap-1 order-last sm:order-first">
      <button class="btn btn-outline btn-success" @click="open = !open">
        <span class="icon text-lg"><i class="mdi mdi-filter-variant" /></span>
      </button>
      <button v-tooltip="t('bulk.toggle')" class="btn btn-outline btn-primary" @click="showSelect = !showSelect">
        <span class="icon text-lg"><i class="mdi mdi-pencil" /></span>
      </button>
      <button v-if="showSelect" v-tooltip="t('bulk.select_all')" class="btn btn-outline btn-accent" @click="selectAll = !selectAll">
        <span class="icon text-lg"><i class="mdi mdi-checkbox-multiple-marked" /></span>
      </button>
      <button v-if="showSelect && checkedCards.length > 0" v-tooltip="t('bulk.edit')" class="btn btn-outline btn-info" @click="toggleEdit(checkedCards)">
        <span class="icon text-lg"><i class="mdi mdi-book-edit" /></span>
      </button>

      <button
        v-if="userId == null"
        v-tooltip="customSortMode ? t('sorting.exit_custom_sort') : t('sorting.custom_sort')"
        class="btn btn-outline"
        :class="customSortMode ? 'btn-warning' : 'btn-secondary'"
        @click="toggleCustomSort"
      >
        <span class="icon text-lg"><i class="mdi mdi-sort" /></span>
      </button>

      <button
        v-if="customSortMode"
        v-tooltip="t('sorting.save_custom_sort')"
        class="btn btn-success"
        :class="{ loading: isSavingSortOrder }"
        :disabled="isSavingSortOrder"
        @click="saveCustomSortOrder"
      >
        <span class="icon text-lg"><i class="mdi mdi-content-save" /></span>
      </button>
    </div>

    <h2 class="text-3xl capitalize" :class="typographyClasses">
      {{ message }} :
    </h2>
    <div />
  </div>

  <div v-if="customSortMode" class="alert alert-info mb-3">
    <span class="icon"><i class="mdi mdi-information-outline" /></span>
    <span>{{ t('sorting.drag_to_reorder') }}</span>
  </div>

  <o-pagination
    v-if="!customSortMode && books.length > 0 && pageCount > 1"
    v-model:current="pageAsNumber"
    :total="total"
    order="centered"
    :per-page="perPage"
    @change="updatePage"
  />

  <div
    v-if="books.length > 0"
    ref="gridRef"
    class="grid grid-cols-2 md:grid-cols-4 xl:grid-cols-6 2xl:grid-cols-8 gap-0 my-3"
  >
    <div
      v-for="book in books"
      :key="book.id"
      class="books-grid-item m-2"
      :class="{ 'cursor-grab': customSortMode }"
    >
      <book-card
        :book="book"
        :force-select="selectAll"
        :public="false"
        :show-select="showSelect && !customSortMode"
        :propose-add="userId == null && !customSortMode"
        class="h-full"
        :class="{ 'pointer-events-none': customSortMode }"
        @update:modal-closed="modalClosed"
        @update:checked="cardChecked"
      />
    </div>
  </div>

  <div v-else-if="getToReadIsLoading" class="flex flex-row justify-center justify-items-center gap-3">
    <o-skeleton class="justify-self-center basis-36" height="250px" :animated="true" />
    <o-skeleton class="justify-self-center basis-36" height="250px" :animated="true" />
    <o-skeleton class="justify-self-center basis-36" height="250px" :animated="true" />
  </div>

  <div v-else>
    <h2 class="text-3xl" :class="typographyClasses">{{ t('labels.nothing_to_read') }}</h2>
    <span class="icon"><i class="mdi mdi-book-open-page-variant-outline mdi-48px" /></span>
  </div>

  <o-pagination
    v-if="!customSortMode && books.length > 0"
    v-model:current="pageAsNumber"
    :total="total"
    order="centered"
    :per-page="perPage"
    @change="updatePage"
  />

  <o-loading v-model:active="getPageIsLoading" :full-page="true" :cancelable="true" />
</template>

<style scoped>
label.label {
  font-weight: bold;
}

.sortable-ghost {
  opacity: 0.3;
}
.sortable-drag {
  cursor: grabbing !important;
}
</style>