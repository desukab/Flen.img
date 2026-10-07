use std::ffi::CString;
use std::os::raw::c_char;
#[no_mangle]
pub extern "C" fn flen_engine_info() -> *mut c_char { CString::new("Flen native engine 0.1").unwrap().into_raw() }
#[no_mangle]
pub unsafe extern "C" fn flen_engine_free(ptr: *mut c_char) { if !ptr.is_null() { drop(CString::from_raw(ptr)); } }
#[no_mangle]
pub unsafe extern "C" fn Java_app_flenimg_NativeEngine_nativeInfo(_env: *mut core::ffi::c_void, _class: *mut core::ffi::c_void) -> *mut c_char { flen_engine_info() }
