<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import useDates from '../composables/dates';
import { Metadata } from "../model/Metadata";

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })
const { formatDateString } = useDates()

defineProps<{
  metadata: Metadata,
}>()

</script>

<template>
  <section>
    <div class="grid grid-cols-5 justify-center justify-items-center justify-self-center">
      <div
        class="col-span-1 mr-3"
      >
        <figure>
          <img
            :src="metadata?.image?.startsWith('http') ? metadata?.image : '/files/' + metadata?.image"
            alt="cover image"
          >
        </figure>
      </div>
      <div
        class="col-span-4"
      >
        <p
          v-if="metadata?.title"
          class="mb-2"
        >
          <span class="font-semibold capitalize">{{ t('book.title') }} : </span>{{ metadata.title }}
        </p>
        <p
          v-if="metadata?.authors != null && metadata?.authors?.length > 0"
        >
          <span class="font-semibold capitalize">{{ t('book.author', 2) }} : </span>
        </p>
        <ul
          v-if="metadata?.authors != null && metadata?.authors?.length > 0"
        >
          <li
            v-for="author in metadata?.authors"
            :key="author"
          >
            {{ author }}
          </li>
        </ul>
        <p
          v-if="metadata?.publisher"
          class="my-2"
        >
          <span class="font-semibold capitalize">{{ t('book.publisher') }} : </span>{{ metadata.publisher }}
        </p>
        <p
          v-if="metadata?.isbn10"
          class="mb-2"
        >
          <span class="font-semibold uppercase">{{ t('book.isbn10') }} : </span>{{ metadata.isbn10 }}
        </p>
        <p
          v-if="metadata?.isbn13"
          class="mb-2"
        >
          <span class="font-semibold uppercase">{{ t('book.isbn13') }} : </span>{{ metadata.isbn13 }}
        </p>
        <p
          v-if="metadata?.pageCount"
          class="mb-2"
        >
          <span class="font-semibold capitalize">{{ t('book.page', 2) }} : </span>{{ metadata.pageCount }}
        </p>
        <p
          v-if="metadata?.publishedDate"
          class="mb-2"
        >
          <span class="font-semibold capitalize">{{ t('book.published_date') }} : </span>{{ formatDateString(metadata.publishedDate) }}
        </p>
        <p
          v-if="metadata?.series"
          class="mb-2"
        >
          <span class="font-semibold capitalize">{{ t('book.series') }} : </span>{{ metadata.series }}
        </p>
        <p
          v-if="metadata?.numberInSeries"
          class="mb-2"
        >
          <span class="font-semibold">{{ t('book.nb_in_series') }} : </span>{{ metadata.numberInSeries }}
        </p>
        <p
          v-if="metadata?.language"
          class="mb-2"
        >
          <span class="font-semibold capitalize">{{ t('book.language') }} : </span>{{ metadata.language }}
        </p>
        <p
          v-if="metadata?.goodreadsId"
          class="mb-2"
        >
          <span class="font-semibold">{{ t('book.goodreads_id') }} : </span>{{ metadata.goodreadsId }}
        </p>
        <p
          v-if="metadata?.googleId"
          class="mb-2"
        >
          <span class="font-semibold">{{ t('book.google_id') }} : </span>{{ metadata.googleId }}
        </p>
        <p
          v-if="metadata?.amazonId"
          class="mb-2"
        >
          <span class="font-semibold">{{ t('book.amazon_id') }} : </span>{{ metadata.amazonId }}
        </p>
        <p
          v-if="metadata?.tags != null && metadata?.tags?.length > 0"
        >
          <span class="font-semibold capitalize">{{ t('book.tag', 2) }} : </span>
        </p>
        <p v-if="metadata?.tags != null && metadata?.tags?.length > 0">
          <span
            v-for="tag in metadata?.tags"
            :key="tag"
            class="badge badge-accent badge-outline font-semibold border-2"
          >{{ tag }}&nbsp;</span>
        </p>
        <p
          v-if="metadata?.summary"
          class="my-2"
        >
          <span class="font-semibold capitalize">{{ t('book.summary') }} : </span>{{ metadata.summary }}
        </p>
      </div>
    </div>
  </section>
</template>

<style lang="scss">

</style>
