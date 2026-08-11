import '@testing-library/jest-dom/vitest'

// Recharts measures its container through ResizeObserver, which jsdom
// does not implement.
class ResizeObserverStub {
  observe() {}
  unobserve() {}
  disconnect() {}
}

if (!globalThis.ResizeObserver) {
  globalThis.ResizeObserver = ResizeObserverStub
}
