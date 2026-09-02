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
    const avatar = avatars[userId] || ''
    if (avatar) {
      console.log(`avatarManager.getAvatar(${userId}):`, avatar.substring(0, 50) + '...')
    }
    return avatar
  },

  setAvatar(userId, avatarBase64) {
    if (!avatarBase64) return
    
    const avatars = this.getAllAvatars()
    avatars[userId] = avatarBase64
    localStorage.setItem(AVATAR_KEY, JSON.stringify(avatars))
    this.updateTimestamp()
    this.notifyChange()
    console.log(`avatarManager.setAvatar(${userId}) called`)
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
    console.log('avatarManager timestamp updated:', timestamp)
  },

  getTimestamp() {
    return localStorage.getItem(UPDATE_TIMESTAMP_KEY) || '0'
  },

  notifyChange() {
    console.log('avatarManager notifying change')
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
        console.log('avatarManager detected change:', lastTimestamp, '->', currentTimestamp)
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
