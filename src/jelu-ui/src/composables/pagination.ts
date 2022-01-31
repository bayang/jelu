import { useRouteQuery } from '@vueuse/router';
import { computed, Ref, ref } from 'vue';
import { useRoute } from 'vue-router';

export default function usePagination() {
    const route = useRoute()
    console.log(route.query)
    const page: Ref<string> = useRouteQuery('page', '1')
    const total: Ref<number> = ref(0)
    const perPage: Ref<number> = ref(24)
    const updatePage = (newVal: number) => {
        page.value = newVal.toString(10)
    }
    // despite what oruga doc says, if page is a string, it returns page +1
    // on clicking next, so for page 1, 1+1 eq 11 ... and so on
    // instead of 1+1 eq 2, so return a number prop
    const pageAsNumber = computed(() => Number.parseInt(page.value))
    return {
        total,
        page,
        pageAsNumber,
        perPage,
        updatePage
    }
}
