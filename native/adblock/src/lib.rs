use adblock::lists::{FilterSet, ParseOptions};
use adblock::request::Request;
use adblock::resources::{PermissionMask, Resource};
use adblock::Engine;
use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jstring, JNI_FALSE, JNI_TRUE};
use jni::JNIEnv;
use serde::Serialize;
use std::collections::HashSet;
use std::ptr;
use std::sync::{OnceLock, RwLock};

static ENGINE: OnceLock<RwLock<Option<Engine>>> = OnceLock::new();

fn engine_slot() -> &'static RwLock<Option<Engine>> {
    ENGINE.get_or_init(|| RwLock::new(None))
}

fn read_java_string(env: &mut JNIEnv, value: JString) -> Option<String> {
    env.get_string(&value).ok().map(|value| value.into())
}

fn java_string(env: &mut JNIEnv, value: &str) -> jstring {
    env.new_string(value)
        .map(|value| value.into_raw())
        .unwrap_or(ptr::null_mut())
}

fn with_engine<T>(f: impl FnOnce(&Engine) -> T) -> Option<T> {
    let guard = engine_slot().read().ok()?;
    guard.as_ref().map(f)
}

#[derive(Serialize)]
struct NetworkDecision {
    block: bool,
    redirect: Option<String>,
    rewritten_url: Option<String>,
}

#[no_mangle]
pub extern "system" fn Java_io_legado_app_help_site_PrivateAdBlock_nativeInitialize(
    mut env: JNIEnv,
    _class: JClass,
    filters: JString,
    resources: JString,
) -> jboolean {
    let Some(filters) = read_java_string(&mut env, filters) else {
        return JNI_FALSE;
    };
    let resources = read_java_string(&mut env, resources).unwrap_or_default();

    let mut filter_set = FilterSet::new(false);
    filter_set.add_filter_list(
        filters,
        ParseOptions {
            // The packaged lists are pinned, curated first-party assets. Allow their trusted
            // uBO/Brave scriptlets to use the permission level expected by Brave's default list.
            permissions: PermissionMask::from_bits(1),
            ..Default::default()
        },
    );

    let mut engine = Engine::new_with_filter_set(filter_set);
    if !resources.trim().is_empty() {
        let Ok(resources) = serde_json::from_str::<Vec<Resource>>(&resources) else {
            return JNI_FALSE;
        };
        engine.use_resources(resources);
    }

    let Ok(mut guard) = engine_slot().write() else {
        return JNI_FALSE;
    };
    *guard = Some(engine);
    JNI_TRUE
}

#[no_mangle]
pub extern "system" fn Java_io_legado_app_help_site_PrivateAdBlock_nativeIsReady(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    if with_engine(|_| ()).is_some() {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

#[no_mangle]
pub extern "system" fn Java_io_legado_app_help_site_PrivateAdBlock_nativeNetworkDecision(
    mut env: JNIEnv,
    _class: JClass,
    url: JString,
    source_url: JString,
    request_type: JString,
    method: JString,
) -> jstring {
    let Some(url) = read_java_string(&mut env, url) else {
        return java_string(&mut env, "{}");
    };
    let source_url = read_java_string(&mut env, source_url).unwrap_or_default();
    let request_type = read_java_string(&mut env, request_type).unwrap_or_else(|| "other".into());
    let method = read_java_string(&mut env, method).unwrap_or_else(|| "get".into());

    let decision = with_engine(|engine| {
        let request = Request::new(&url, &source_url, &request_type, &method).ok()?;
        let result = engine.check_network_request(&request);
        Some(NetworkDecision {
            block: result.should_block(),
            redirect: result.redirect,
            rewritten_url: result.rewritten_url,
        })
    })
    .flatten();

    let json = decision
        .and_then(|decision| serde_json::to_string(&decision).ok())
        .unwrap_or_else(|| "{}".to_string());
    java_string(&mut env, &json)
}

#[no_mangle]
pub extern "system" fn Java_io_legado_app_help_site_PrivateAdBlock_nativeCosmeticResources(
    mut env: JNIEnv,
    _class: JClass,
    url: JString,
) -> jstring {
    let Some(url) = read_java_string(&mut env, url) else {
        return java_string(&mut env, "{}");
    };
    let json = with_engine(|engine| engine.url_cosmetic_resources(&url))
        .and_then(|resources| serde_json::to_string(&resources).ok())
        .unwrap_or_else(|| "{}".to_string());
    java_string(&mut env, &json)
}

#[no_mangle]
pub extern "system" fn Java_io_legado_app_help_site_PrivateAdBlock_nativeHiddenSelectors(
    mut env: JNIEnv,
    _class: JClass,
    classes_json: JString,
    ids_json: JString,
    exceptions_json: JString,
) -> jstring {
    let classes_json = read_java_string(&mut env, classes_json).unwrap_or_else(|| "[]".into());
    let ids_json = read_java_string(&mut env, ids_json).unwrap_or_else(|| "[]".into());
    let exceptions_json =
        read_java_string(&mut env, exceptions_json).unwrap_or_else(|| "[]".into());

    let classes = serde_json::from_str::<Vec<String>>(&classes_json).unwrap_or_default();
    let ids = serde_json::from_str::<Vec<String>>(&ids_json).unwrap_or_default();
    let exceptions = serde_json::from_str::<HashSet<String>>(&exceptions_json).unwrap_or_default();

    let selectors = with_engine(|engine| {
        engine.hidden_class_id_selectors(classes.iter(), ids.iter(), &exceptions)
    })
    .unwrap_or_default();

    let json = serde_json::to_string(&selectors).unwrap_or_else(|_| "[]".to_string());
    java_string(&mut env, &json)
}
