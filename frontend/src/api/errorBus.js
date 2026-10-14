/**
 * Lightweight, decoupled event bus for centralized API error broadcasting.
 * UI components can subscribe to global errors without coupling directly to Axios.
 */
class ErrorBus {
  constructor() {
    this.listeners = new Set();
  }

  /**
   * Subscribe to global API error events
   * @param {Function} callback (errorData) => void
   * @returns {Function} unsubscribe function
   */
  subscribe(callback) {
    this.listeners.add(callback);
    return () => {
      this.listeners.delete(callback);
    };
  }

  /**
   * Emit an error event to all active subscribers
   * @param {Object} errorData Standardized error payload
   */
  emit(errorData) {
    this.listeners.forEach((callback) => {
      try {
        callback(errorData);
      } catch (err) {
        console.error('Error in ErrorBus listener:', err);
      }
    });
  }
}

export const errorBus = new ErrorBus();
