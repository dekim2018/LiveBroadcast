import { defineStore } from 'pinia'

let seq = 0

export const useToastStore = defineStore('toast', {
  state: () => ({ items: [] }),
  actions: {
    show(message, type = 'info', duration = 3500) {
      const id = ++seq
      this.items.push({ id, message, type })
      setTimeout(() => this.dismiss(id), duration)
    },
    success(message) { this.show(message, 'success') },
    error(message) { this.show(message, 'error', 5000) },
    dismiss(id) {
      this.items = this.items.filter((t) => t.id !== id)
    }
  }
})
