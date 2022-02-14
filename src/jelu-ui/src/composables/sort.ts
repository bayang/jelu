import { useRouteQuery } from '@vueuse/router';
import { ref, Ref, watch } from 'vue';
import { useRoute } from 'vue-router';

export default function useSort(defaultSort: string) {
    const route = useRoute()
    console.log(route.query)
    const sortQuery: Ref<string> = useRouteQuery('sort', defaultSort)

    const {field, order} = splitVal(sortQuery.value)

    const sortOrder = ref(order)

    const sortBy = ref(field)

    watch([sortBy, sortOrder], (newVal, oldVal) => {
        console.log("sort " + newVal + " " + oldVal)
        if (newVal !== oldVal) {
          sortQuery.value = newVal.join(",")
        }
      })

    const sortOrderUpdated = (newval: string) => {
        console.log('sortOrderUpdated ' + newval)
        sortOrder.value = newval
      }

    return {
        sortQuery,
        sortOrder,
        sortBy,
        sortOrderUpdated
    }
}

const splitVal = (input: string) => {
    const ret = input.split(",")
    return {"field" :ret[0], "order" : ret[1]}
  }