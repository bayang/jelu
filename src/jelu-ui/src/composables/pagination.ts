import { useRouteQuery } from '@vueuse/router';
import { computed, Ref, ref } from 'vue';
import { useRoute } from 'vue-router';
import { onKeyStroke } from '@vueuse/core'

export default function usePagination() {
    const route = useRoute()
    console.log(route.query)
    const page: Ref<string> = useRouteQuery('page', '1')
    const total: Ref<number> = ref(0)
    const perPage: Ref<number> = ref(24)
    const pageCount = computed(() => {
        return Math.ceil(total.value / perPage.value)
    })
    const updatePage = (newVal: number) => {
        if (newVal < 1 || newVal > pageCount.value) {
            return
        }
        page.value = newVal.toString(10)
    }
    // despite what oruga doc says, if page is a string, it returns page +1
    // on clicking next, so for page 1, 1+1 eq 11 ... and so on
    // instead of 1+1 eq 2, so return a number prop
    const pageAsNumber = computed(() => Number.parseInt(page.value))

    onKeyStroke('ArrowLeft', (e) => {
        e.preventDefault()
        updatePage(pageAsNumber.value - 1)
    })

    onKeyStroke('ArrowRight', (e) => {
        e.preventDefault()
        updatePage(pageAsNumber.value + 1)
    })

    return {
        total,
        page,
        pageAsNumber,
        perPage,
        updatePage
    }
}
