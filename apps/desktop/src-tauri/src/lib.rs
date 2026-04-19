use std::fs;
use std::path::{Component, Path, PathBuf};

fn ensure_root_dir(root_path: &str) -> Result<PathBuf, String> {
    let trimmed = root_path.trim();
    if trimmed.is_empty() {
        return Err("本地副本目录不能为空".to_string());
    }
    let root = PathBuf::from(trimmed);
    fs::create_dir_all(&root).map_err(|error| error.to_string())?;
    Ok(root)
}

fn sanitize_relative_path(relative_path: &str) -> Result<PathBuf, String> {
    let normalized = relative_path.trim().replace('\\', "/").trim_matches('/').to_string();
    if normalized.is_empty() {
        return Ok(PathBuf::new());
    }

    let mut sanitized = PathBuf::new();
    for component in Path::new(&normalized).components() {
        match component {
            Component::Normal(part) => sanitized.push(part),
            Component::CurDir => {}
            _ => return Err("同步路径必须位于本地副本目录之内".to_string()),
        }
    }
    Ok(sanitized)
}

fn resolve_sync_path(root_path: &str, relative_path: &str) -> Result<PathBuf, String> {
    let root = ensure_root_dir(root_path)?;
    let relative = sanitize_relative_path(relative_path)?;
    Ok(root.join(relative))
}

#[tauri::command]
fn sync_status() -> &'static str {
    "desktop-agent-ready"
}

#[tauri::command]
fn ensure_sync_root(root_path: String) -> Result<String, String> {
    let root = ensure_root_dir(&root_path)?;
    Ok(root.to_string_lossy().to_string())
}

#[tauri::command]
fn ensure_sync_directory(root_path: String, relative_path: String) -> Result<String, String> {
    let target = resolve_sync_path(&root_path, &relative_path)?;
    fs::create_dir_all(&target).map_err(|error| error.to_string())?;
    Ok(target.to_string_lossy().to_string())
}

#[tauri::command]
fn write_sync_file(root_path: String, relative_path: String, content: String) -> Result<String, String> {
    let target = resolve_sync_path(&root_path, &relative_path)?;
    if let Some(parent) = target.parent() {
        fs::create_dir_all(parent).map_err(|error| error.to_string())?;
    }
    fs::write(&target, content).map_err(|error| error.to_string())?;
    Ok(target.to_string_lossy().to_string())
}

#[tauri::command]
fn move_sync_path(root_path: String, source_path: String, target_path: String) -> Result<String, String> {
    let source = resolve_sync_path(&root_path, &source_path)?;
    let target = resolve_sync_path(&root_path, &target_path)?;
    if !source.exists() {
        return Err("源路径不存在，无法执行移动".to_string());
    }
    if let Some(parent) = target.parent() {
        fs::create_dir_all(parent).map_err(|error| error.to_string())?;
    }
    fs::rename(&source, &target).map_err(|error| error.to_string())?;
    Ok(target.to_string_lossy().to_string())
}

#[tauri::command]
fn delete_sync_path(root_path: String, relative_path: String) -> Result<String, String> {
    let target = resolve_sync_path(&root_path, &relative_path)?;
    if !target.exists() {
        return Ok("missing".to_string());
    }
    if target.is_dir() {
        fs::remove_dir_all(&target).map_err(|error| error.to_string())?;
    } else {
        fs::remove_file(&target).map_err(|error| error.to_string())?;
    }
    Ok(target.to_string_lossy().to_string())
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .invoke_handler(tauri::generate_handler![
            sync_status,
            ensure_sync_root,
            ensure_sync_directory,
            write_sync_file,
            move_sync_path,
            delete_sync_path
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
