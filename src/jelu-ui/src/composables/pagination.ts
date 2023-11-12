import { useRouteQuery } from '@vueuse/router';
import { computed, Ref, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useMagicKeys } from '@vueuse/core'

const keys = useMagicKeys()
const shiftLeft = keys['Shift+Left']
const shiftRight = keys['Shift+Right']

export default function usePagination(pageSize = 24) {
    const route = useRoute()
    console.log(route.query)
    const page: Ref<string> = useRouteQuery('page', '1')
    const total: Ref<number> = ref(0)
    const perPage: Ref<number> = ref(pageSize)
    const getPageIsLoading: Ref<boolean> = ref(false)
    const pageCount = computed(() => {
        return Math.ceil(total.value / perPage.value)
    })
    const updatePage = (newVal: number) => {
        if (newVal < 1 || newVal > pageCount.value) {
            return
        }
        page.value = newVal.toString(10)
        getPageIsLoading.value = true
    }
    const updatePageLoading = (newVal: boolean) => {
        getPageIsLoading.value = newVal
    }
    // despite what oruga doc says, if page is a string, it returns page +1
    // on clicking next, so for page 1, 1+1 eq 11 ... and so on
    // instead of 1+1 eq 2, so return a number prop
    const pageAsNumber = computed(() => Number.parseInt(page.value))

    watch(shiftLeft, (v) => {
        if (v) {
            updatePage(pageAsNumber.value - 1)
        }
      })

    watch(shiftRight, (v) => {
        if (v) {
            updatePage(pageAsNumber.value + 1)
        }
      })

    return {
        total,
        page,
        pageAsNumber,
        perPage,
        updatePage,
        getPageIsLoading,
        updatePageLoading
    }
}
