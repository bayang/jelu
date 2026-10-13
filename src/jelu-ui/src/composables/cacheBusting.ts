import { Ref, ref } from 'vue'

export default function useCacheBusting() {
    
    const generateTimestamp = (): string => {
        return new Date().toISOString()
    }

    const currentTimestamp: Ref<string> = ref(generateTimestamp())

    const refreshTimestamp = (): void => {
        currentTimestamp.value = generateTimestamp()
    }

    const getCachebustedUrl = (imageUrl: string): string => {
        if (!imageUrl) {
            return imageUrl
        }
        return `${imageUrl}?timestamp=${currentTimestamp.value}`
    }

    return {
        currentTimestamp,
        refreshTimestamp,
        getCachebustedUrl,
        generateTimestamp
    }
}
