<script setup lang="ts">
import { setErrors } from '@formkit/vue';
import { useOruga } from "@oruga-ui/oruga-next";
import { ref } from 'vue';
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

const createUser = ref({"password" : "", "isAdmin" : undefined})
console.log(props.currentUser)
console.log(createUser)

const emit = defineEmits<{
  (e: 'close'): void
}>()

async function editUser(user: any) {
  console.log("edit user")
  console.log(createUser)
  console.log(user)
  if (props.currentUser.id != null) {
    try {
      let modified = await dataService.updateUser(props.currentUser.id, {"isAdmin": user.isAdmin, "password": user.password})
      store.commit('user', modified)
      ObjectUtils.toast(oruga, "success", t('admin_user.user_updated', {name : props.currentUser.login}), 2500)
      emit('close')
    } catch (err: any) {
      setErrors('edit-user-form', [], err.message)
    }
  }
}

</script>

<template>
  <section class="event-modal">
    <div>
      <h1 class="typewriter text-2xl first-letter:capitalize">
        {{ t('admin_user.edit_user', {name : props.currentUser.login}) }}
      </h1>
      <div>
        <FormKit
          id="edit-user-form"
          v-slot="{ state: { valid } }"
          v-model="createUser"
          type="form"
          :actions="false"
          message-class="text-error-content"
          messages-class="alert alert-error mt-2"
          @submit="editUser"
        >
          <FormKit
            type="password"
            name="password"
            :label="t('admin_user.password')"
            validation="required|length:3"
            :placeholder="t('admin_user.password')"
          />
          <FormKit
            type="password"
            name="password_confirm"
            :label="t('admin_user.password_confirm')"
            :placeholder="t('admin_user.password_confirm')"
            validation="required|confirm"
          />
          <FormKit
            type="submit"
            :disabled="!valid"
            input-class="btn-accent mt-3"
          >
            {{ t('labels.submit') }}
          </FormKit>
        </FormKit>
      </div>
    </div>
  </section>
</template>

<style lang="scss">

</style>
