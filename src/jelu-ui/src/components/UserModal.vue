<script setup lang="ts">
import { useOruga } from "@oruga-ui/oruga-next";
import { computed, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useStore } from 'vuex';
import { User } from "../model/User";
import dataService from "../services/DataService";
import { key } from '../store';
import { ObjectUtils } from "../utils/ObjectUtils";

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const oruga = useOruga()
const store = useStore(key)

const props = defineProps<{
  currentUser: User,
}>()

const form = ref({'password' : '', 'password_confirm': ''})
console.log(props.currentUser)

const emit = defineEmits<{
  (e: 'close'): void
}>()

async function editUser() {
  console.log("edit user")
  console.log(form)
  if (props.currentUser.id != null) {
    try {
      const modified = await dataService.updateUser(props.currentUser.id, {"isAdmin": undefined, "password": form.value.password})
      store.commit('user', modified)
      ObjectUtils.toast(oruga, "success", t('admin_user.user_updated', {name : props.currentUser.login}), 2500)
      emit('close')
    } catch (err: any) {
      console.log('failed to edit user')
      console.log(err)
    }
  }
}

const isValid = computed(() => {
  return form.value.password.length >=3 && form.value.password_confirm.length >=3 && form.value.password === form.value.password_confirm
})
</script>

<template>
  <section class="event-modal">
    <div>
      <h1 class="typewriter text-2xl first-letter:capitalize">
        {{ t('admin_user.edit_user', {name : props.currentUser.login}) }}
      </h1>
      <div>
        <label class="fieldset">
          <span class="label capitalize">{{ t('admin_user.password') }}</span>
          <input
            v-model="form.password"
            type="password"
            class="input validator"
            :placeholder="t('admin_user.password')"
            required
            minlength="3"
          >
          <span class="validator-hint hidden">{{ t('login.password_length') }}</span>
        </label>
        <label class="fieldset">
          <span class="label capitalize">{{ t('admin_user.password_confirm') }}</span>
          <input
            v-model="form.password_confirm"
            type="password"
            class="input validator"
            :placeholder="t('admin_user.password_confirm')"
            required
            minlength="3"
          >
          <span class="validator-hint hidden">{{ t('login.password_length') }}</span>
        </label>
        <button
          class="btn btn-accent mt-4"
          type="submit"
          :disabled="! isValid"
          @click="editUser"
        >
          {{ t('labels.submit') }}
        </button>
      </div>
    </div>
  </section>
</template>

<style lang="scss">

</style>
