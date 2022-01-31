<script setup lang="ts">
import { useProgrammatic } from "@oruga-ui/oruga-next";
import { computed, onMounted, Ref, ref, watch } from 'vue';
import usePagination from '../composables/pagination';
import { BookWithUserBook, UserBook } from '../model/Book';
import { Tag } from '../model/Tag';
import dataService from "../services/DataService";
import { ObjectUtils } from '../utils/ObjectUtils';
import BookCard from "./BookCard.vue";
import EditBookModal from "./EditBookModal.vue";

const {oruga} = useProgrammatic();

const props = defineProps<{ tagId: string }>()

const tag: Ref<Tag> = ref({name: ""})
const tagBooks: Ref<Array<BookWithUserBook>> = ref([]);
const edit: Ref<boolean> = ref(false)

const { total, page, pageAsNumber, perPage, updatePage } = usePagination()

watch(page, (newVal, oldVal) => {
  console.log(page.value)
  console.log(newVal + " " + oldVal)
  if (newVal !== oldVal) {
    getBooks()
  }
})

const getTag = async () => {
  try {
    tag.value = await dataService.getTagById(props.tagId)
  } catch (error) {
    console.log("failed get tag : " + error);
  }
};

const getBooks = () => {
  dataService.getTagBooksById(props.tagId, 
    Number.parseInt(page.value) - 1, perPage.value)
    .then(res => {
        console.log(res)
          total.value = res.totalElements
          tagBooks.value = res.content
        if (! res.empty) {
          page.value =  (res.number + 1).toString(10)
        }
        else {
          page.value = "1"
        }
    }
    )
  
};

onMounted(() => {
  console.log("Component is mounted!");
    
    }
);

const convertedBooks = computed(() => tagBooks.value?.map(b => ObjectUtils.toUserBook(b)))

function modalClosed() {
  console.log("modal closed")
  getBooks()
}

const toggleEdit = (book: UserBook) => {
  edit.value = ! edit.value
  console.log("book")
  console.log(book)
  oruga.modal.open({
    // parent: this,
          component: EditBookModal,
          trapFocus: true,
          active: true,
          // fullScreen: false,
          canCancel: ['x', 'button', 'outside'],
          scroll: 'clip',
          props: {
            "book" : book
          },
          onClose: modalClosed
        });
}

getTag()
getBooks()

</script>

<template>
  <h2 class="title has-text-weight-normal typewriter">
    Books tagged #{{ tag.name }} :
  </h2>
  <div class="is-flex is-flex-wrap-wrap is-justify-content-space-evenly">
    <div
      v-for="book in convertedBooks"
      :key="book.id"
      class="books-grid-item my-2"
    >
      <router-link
        v-if="book.id != undefined"
        :to="{ name: 'book-detail', params: { bookId: book.id } }"
      >
        <book-card :book="book" />
      </router-link>
      <div v-else>
        <book-card
          v-tooltip="'This book is not yet in your books, double click to add it'"
          :book="book"
          @dblclick="toggleEdit(book)"
        >
          <template #icon>
            <o-tooltip
              label="not in your books"
              variant="danger"
            >
              <span class="icon has-text-danger">
                <i class="mdi mdi-plus-circle mdi-18px" />
              </span>
            </o-tooltip>
          </template>
        </book-card>
      </div>
    </div>
  </div>
  <o-pagination
    v-model:current="pageAsNumber"
    :total="total"
    order="centered"
    :per-page="perPage"
    @change="updatePage"
  />
</template>

<style scoped>


</style>
