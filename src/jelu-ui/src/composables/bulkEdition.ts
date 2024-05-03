import { useOruga } from "@oruga-ui/oruga-next";
import { Ref, ref } from 'vue';
import BulkEditModal from "../components/BulkEditModal.vue";

type VoidFunc = () => void;

export default function useBulkEdition(onModalClosed: VoidFunc) {

    const oruga = useOruga();
    const showSelect = ref(false)
    const selectAll = ref(false)
    const checkedCards: Ref<Array<string>> = ref([])

    const cardChecked = (id: string | null, checked: boolean) => {
        console.log(`received ${id}, checked ? ${checked}`)
        if (id != null) {
            if (checked) {
                if (!checkedCards.value.includes(id)) {
                    checkedCards.value.push(id)
                }
            } else {
                checkedCards.value.forEach((item, index) => {
                    if (item === id) checkedCards.value.splice(index, 1);
                });
            }
        }
        console.log(`ids ${checkedCards.value}`)
    }

    function modalClosed() {
        console.log("modal closed from bulk composable")
        onModalClosed()
    }

    const toggleEdit = (ids: Array<string>) => {
        if (ids != null) {
          console.log("ids")
          console.log(ids)
          oruga.modal.open({
                  component: BulkEditModal,
                  trapFocus: true,
                  active: true,
                  canCancel: ['x', 'button', 'outside'],
                  scroll: 'clip',
                  props: {
                    "ids" : ids,
                  },
                  onClose: modalClosed
                });
        }
      }

    return {
        showSelect,
        selectAll,
        checkedCards,
        cardChecked,
        toggleEdit,
    }
}
