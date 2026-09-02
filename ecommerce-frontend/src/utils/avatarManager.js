const AVATAR_KEY = 'user_avatars'
const UPDATE_TIMESTAMP_KEY = 'avatar_update_timestamp'

export const avatarManager = {
  getAllAvatars() {
    try {
      const data = localStorage.getItem(AVATAR_KEY)
      return data ? JSON.parse(data) : {}
    } catch (e) {
      console.error('Error parsing avatars:', e)
      return {}
    }
  },

  getAvatar(userId) {
    const avatars = this.getAllAvatars()
    return avatars[userId] || ''
  },

  setAvatar(userId, avatarBase64) {
    if (!avatarBase64) return
    
    const avatars = this.getAllAvatars()
    avatars[userId] = avatarBase64
    localStorage.setItem(AVATAR_KEY, JSON.stringify(avatars))
    this.updateTimestamp()
    this.notifyChange()
    console.log(`avatarManager.setAvatar(${userId})`)
  },

  removeAvatar(userId) {
    const avatars = this.getAllAvatars()
    delete avatars[userId]
    localStorage.setItem(AVATAR_KEY, JSON.stringify(avatars))
    this.updateTimestamp()
    this.notifyChange()
  },

  updateTimestamp() {
    const timestamp = Date.now().toString()
    localStorage.setItem(UPDATE_TIMESTAMP_KEY, timestamp)
  },

  getTimestamp() {
    return localStorage.getItem(UPDATE_TIMESTAMP_KEY) || '0'
  },

  notifyChange() {
    const event = new CustomEvent('avatarUpdated', { 
      detail: { timestamp: this.getTimestamp() } 
    })
    window.dispatchEvent(event)
    
    window.dispatchEvent(new StorageEvent('storage', {
      key: AVATAR_KEY,
      newValue: localStorage.getItem(AVATAR_KEY)
    }))
  },

  subscribe(callback) {
    const handler = (event) => {
      callback(event.detail)
    }
    window.addEventListener('avatarUpdated', handler)
    return () => {
      window.removeEventListener('avatarUpdated', handler)
    }
  },

  watch(callback, interval = 500) {
    let lastTimestamp = this.getTimestamp()
    const intervalId = setInterval(() => {
      const currentTimestamp = this.getTimestamp()
      if (currentTimestamp !== lastTimestamp) {
        lastTimestamp = currentTimestamp
        callback()
      }
    }, interval)
    return () => {
      clearInterval(intervalId)
    }
  }
}

export default avatarManager
