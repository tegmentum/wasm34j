;; Exports memory plus load/store helpers for the memory tests.
(module
  (memory (export "memory") 1)
  (func (export "load") (param i32) (result i32)
    local.get 0
    i32.load)
  (func (export "store") (param i32 i32)
    local.get 0
    local.get 1
    i32.store))
