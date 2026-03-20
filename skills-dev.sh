#!/usr/bin/env bash
# Skills 初始化工具 (单模板)
# 作用: 列出 skills/ 一级目录 -> 选择 -> 递归下载 -> 写入 $BASE_DIR/skills (剥离前两级, 覆盖同名)
# 兼容 Windows Git Bash 和 macOS/Linux

set -e

# 配置（请按需修改）
TOKEN="GKypRi12_8w-vRmfaXea"
PROJECT="one/agents"
REF="dev"
GITLAB_URL="https://gitlab.ruijie.com.cn"

# 颜色定义
RESET='\033[0m'
CYAN='\033[36m'
GREEN='\033[32m'
RED='\033[31m'
DIM='\033[2m'
BOLD='\033[1m'
BLUE='\033[34m'
UNDERLINE='\033[4m'

FIELD_DELIM=$'\x1f'
LIST_DELIM=$'\x1e'

# 检查必要工具
check_dependencies() {
    local missing=()
    if ! command -v curl &> /dev/null; then
        missing+=("curl")
    fi
    if [ ${#missing[@]} -gt 0 ]; then
        echo -e "${RED}错误: 缺少必要工具: ${missing[*]}${RESET}"
        echo "请安装后再运行此脚本。"
        exit 1
    fi
}

# 解析 skills.json 数组，提取 name、description 及 references (纯bash实现)
parse_skills_json() {
    local json="$1"
    local has_item=0

    # 清理 JSON：去除首尾空白，确保是有效的 JSON
    local cleaned_json=$(printf '%s' "$json" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')

    # 验证基本格式：检查是否为数组（以 [ 开头，] 结尾）
    if [[ ! "$cleaned_json" =~ ^\[.*\]$ ]]; then
        echo -e "${RED}错误: skills.json 应该是一个数组${RESET}" >&2
        exit 1
    fi

    # 提取所有 {...} 对象
    local current_object=""
    local brace_count=0
    local i=0
    local len=${#cleaned_json}

    while [ $i -lt $len ]; do
        local char="${cleaned_json:$i:1}"

        if [ "$char" == "{" ]; then
            if [ $brace_count -eq 0 ]; then
                current_object="{"
            else
                current_object="${current_object}${char}"
            fi
            brace_count=$((brace_count + 1))
        elif [ "$char" == "}" ]; then
            brace_count=$((brace_count - 1))
            current_object="${current_object}${char}"

            if [ $brace_count -eq 0 ] && [ -n "$current_object" ]; then
                # 处理单个对象
                local line
                line=$(_parse_single_skill_object "$current_object" "$FIELD_DELIM" "$LIST_DELIM")
                if [ -n "$line" ]; then
                    printf '%s\n' "$line"
                    has_item=1
                fi
                current_object=""
            fi
        elif [ $brace_count -gt 0 ]; then
            current_object="${current_object}${char}"
        fi

        i=$((i + 1))
    done

    if [ $has_item -eq 0 ]; then
        echo -e "${RED}错误: skills.json 为空或缺少必填字段${RESET}" >&2
        exit 1
    fi
}

# 从单个 JSON 对象中提取字段（name, description, references）
_parse_single_skill_object() {
    local obj="$1"
    local field_delim="$2"
    local list_delim="$3"

    local name=""
    local description=""
    local references=""

    # 提取 name
    if [[ "$obj" =~ \"name\"[[:space:]]*:[[:space:]]*\"([^\"]+)\" ]]; then
        name="${BASH_REMATCH[1]}"
    fi

    # 提取 description
    if [[ "$obj" =~ \"description\"[[:space:]]*:[[:space:]]*\"([^\"]+)\" ]]; then
        description="${BASH_REMATCH[1]}"
    fi

    # 提取 references（字符串格式）
    if [[ "$obj" =~ \"references\"[[:space:]]*:[[:space:]]*\"([^\"]+)\" ]]; then
        references="${BASH_REMATCH[1]}"
    fi

    # 只有同时有 name 和 description 时才输出
    if [ -n "$name" ] && [ -n "$description" ]; then
        printf '%s%s%s%s%s\n' "$name" "$field_delim" "$description" "$field_delim" "$references"
    fi
}

# 从 JSON 数组中提取所有符合条件的项目（简化版本）
parse_tree_items() {
    local json="$1"
    local filter_type="$2"
    echo "$json" | grep -o '{[^}]*}' | while IFS= read -r obj; do
        local name=$(echo "$obj" | grep -o '"name"[[:space:]]*:[[:space:]]*"[^\"]*"' | sed 's/"name"[[:space:]]*:[[:space:]]*"\([^\"]*\)"/\1/')
        local type=$(echo "$obj" | grep -o '"type"[[:space:]]*:[[:space:]]*"[^\"]*"' | sed 's/"type"[[:space:]]*:[[:space:]]*"\([^\"]*\)"/\1/')
        if [ -n "$name" ] && [ -n "$type" ]; then
            if [ -z "$filter_type" ] || [ "$type" == "$filter_type" ]; then
                echo "${type}|${name}"
            fi
        fi
    done
}

# URL 编码函数 - 完整编码（包括斜杠）
urlencode_full() {
    local string="$1"
    printf '%s' "$string" | xxd -plain | sed 's/\(..\)/%\1/g' | tr -d '\n'
}

# URL 编码函数 - 保留斜杠
urlencode() {
    local string="$1"
    local encoded=$(printf '%s' "$string" | xxd -plain | sed 's/\(..\)/%\1/g' | tr -d '\n')
    echo "$encoded" | sed 's/%2[fF]/\//g'
}

encode_project_path() {
    local path="$1"
    echo "$path" | sed 's/\//%2F/g'
}
fetch_tree() {
    local project="$1"
    local ref="$2"
    local dir="$3"
    local project_id=$(encode_project_path "$project")
    local url="${GITLAB_URL}/api/v4/projects/${project_id}/repository/tree?ref=${ref}&per_page=100"
    if [ -n "$dir" ]; then
        local encoded_dir=$(urlencode "$dir")
        url="${url}&path=${encoded_dir}"
    fi
    local response=$(curl -s -H "PRIVATE-TOKEN: ${TOKEN}" "$url")
    if [ $? -ne 0 ] || [ -z "$response" ]; then
        echo -e "${RED}错误: 获取目录失败${RESET}" >&2
        return 1
    fi
    echo "$response"
}

fetch_file_content() {
    local project="$1"
    local ref="$2"
    local file_path="$3"
    local project_id=$(encode_project_path "$project")
    local encoded_path=$(urlencode_full "$file_path")
    local url="${GITLAB_URL}/api/v4/projects/${project_id}/repository/files/${encoded_path}/raw?ref=${ref}"
    local tmp_file=$(mktemp)
    http_code=$(curl -s -w "%{http_code}" -H "PRIVATE-TOKEN: ${TOKEN}" "$url" -o "$tmp_file")
    if [ "$http_code" == "200" ]; then
        cat "$tmp_file"
        rm -f "$tmp_file"
        return 0
    else
        echo -e "${RED}错误: 获取文件失败 (HTTP $http_code): $file_path${RESET}" >&2
        if [ "$http_code" == "000" ]; then
            echo -e "${RED}提示: 可能是网络连接问题，请检查网络或 GitLab 服务器是否可访问${RESET}" >&2
        fi
        rm -f "$tmp_file"
        return 1
    fi
}

collect_dir_files() {
    local project="$1"
    local ref="$2"
    local dir="$3"
    local stack=("$dir")
    while [ ${#stack[@]} -gt 0 ]; do
        local last_idx=$((${#stack[@]} - 1))
        local current="${stack[$last_idx]}"
        unset 'stack[$last_idx]'
        local tree=$(fetch_tree "$project" "$ref" "$current")
        if [ $? -ne 0 ]; then
            echo -e "${RED}警告: 获取目录失败: $current${RESET}" >&2
            continue
        fi
        while IFS='|' read -r type name; do
            if [ -z "$type" ] || [ -z "$name" ]; then
                continue
            fi
            if [ "$type" == "tree" ]; then
                stack+=("${current}/${name}")
            elif [ "$type" == "blob" ]; then
                echo "${current}/${name}"
            fi
        done < <(parse_tree_items "$tree" "")
    done
}

array_contains() {
    local seeking="$1"
    shift
    for element in "$@"; do
        if [ "$element" == "$seeking" ]; then
            return 0
        fi
    done
    return 1
}

split_references() {
    local refs_string="$1"
    local normalized_refs="$refs_string"

    normalized_refs="${normalized_refs//、/$'\n'}"
    normalized_refs="${normalized_refs//，/$'\n'}"
    normalized_refs="${normalized_refs//,/$'\n'}"

    while IFS= read -r ref; do
        if [ -n "$ref" ]; then
            printf '%s\n' "$ref"
        fi
    done <<< "$normalized_refs"
}

# 全局变量用于存储选中的索引（兼容旧版 bash）
__SELECTED_INDICES=()

# 交互式多选界面
# 使用全局变量来传递数组（兼容旧版 bash）
interactive_multiselect() {
    # 通过全局变量传递数组名称
    local names_array_name="$1"
    local descriptions_array_name="$2"
    
    # 清空全局结果数组
    __SELECTED_INDICES=()
    
    # 使用 eval 获取数组长度和元素
    eval "local total=\${#${names_array_name}[@]}"
    if [ $total -eq 0 ]; then
        return
    fi
    
    # 初始化选中状态数组
    local selected=()
    for ((i=0; i<total; i++)); do
        selected+=("0")  # 0=未选中, 1=选中
    done
    
    local current=0  # 当前聚焦的索引
    local all_selected=false
    
    # 保存光标位置函数
    save_cursor() {
        tput sc
    }
    
    # 恢复光标位置函数
    restore_cursor() {
        tput rc
    }
    
    # 清屏并显示选项
    render() {
        # 使用 ANSI 转义码清屏（兼容性更好）
        printf "\033[2J\033[H"
        
        # 顶部提示（gum 风格）
        local prompt_text="请选择要安装的 Skills"
        local hint_text="上下键切换，按空格选择，A 全选/取消，回车确定。"
        printf "${CYAN}?${RESET} ${prompt_text} ${DIM}>${RESET} ${DIM}${hint_text}${RESET}\n"
        echo ""
        
        for ((i=0; i<total; i++)); do
            local marker=""
            local name_style=""
            local desc_style=""
            
            # 选中状态指示器
            if [ "${selected[$i]}" == "1" ]; then
                marker="${GREEN}●${RESET}"
            else
                marker="○"
            fi
            
            # 使用 eval 获取数组元素
            eval "local name=\${${names_array_name}[$i]}"
            eval "local description=\${${descriptions_array_name}[$i]}"
            
            # 聚焦状态样式（下划线）
            if [ $i -eq $current ]; then
                name_style="${UNDERLINE}${name}${RESET}"
                desc_style="${description}"
            else
                name_style="${name}"
                desc_style="${description}"
            fi
            
            printf "  ${marker} ${name_style}: ${desc_style}\n"
        done
        
        echo ""
        local selected_count=0
        for ((i=0; i<total; i++)); do
            if [ "${selected[$i]}" == "1" ]; then
                selected_count=$((selected_count + 1))
            fi
        done
        printf "${DIM}已选择: ${selected_count}/${total}${RESET}\n"
    }
    
    # 切换选中状态
    toggle_selection() {
        if [ "${selected[$current]}" == "1" ]; then
            selected[$current]="0"
        else
            selected[$current]="1"
        fi
    }
    
    # 全选/全不选切换
    toggle_all() {
        if [ "$all_selected" == true ]; then
            for ((i=0; i<total; i++)); do
                selected[$i]="0"
            done
            all_selected=false
        else
            for ((i=0; i<total; i++)); do
                selected[$i]="1"
            done
            all_selected=true
        fi
    }
    
    # 初始渲染
    render
    
    # 保存原始终端设置
    local old_stty=$(stty -g 2>/dev/null)
    # 设置终端：禁用规范模式（cbreak），禁用回显
    # 这样可以让 read 立即返回单个字符，而不需要等待换行
    stty -icanon -echo 2>/dev/null || stty cbreak -echo 2>/dev/null || stty -echo 2>/dev/null
    
    # 确保退出时恢复终端设置
    trap "stty $old_stty 2>/dev/null; trap - INT TERM EXIT" INT TERM EXIT
    
    # 读取键盘输入
    while true; do
        # 读取单个字符（不显示）
        local key
        IFS= read -rsn1 key 2>/dev/null || continue
        
        # 处理特殊键（方向键等）
        if [ "$key" == $'\x1b' ]; then
            # 读取下一个字符（方向键序列的第二部分）
            local next_key
            if IFS= read -rsn1 next_key 2>/dev/null; then
                if [ "$next_key" == '[' ]; then
                    # 读取方向键代码（第三部分）
                    if IFS= read -rsn1 next_key 2>/dev/null; then
                        case "$next_key" in
                            'A')  # 上箭头
                                if [ $current -gt 0 ]; then
                                    current=$((current - 1))
                                fi
                                render
                                ;;
                            'B')  # 下箭头
                                if [ $current -lt $((total - 1)) ]; then
                                    current=$((current + 1))
                                fi
                                render
                                ;;
                            *)  # 其他方向键或未知序列，忽略
                                ;;
                        esac
                    fi
                else
                    # ESC 后不是 '['，可能是单独的 ESC 键或其他序列，忽略
                    continue
                fi
            else
                # 读取失败，可能是单独的 ESC 键，忽略
                continue
            fi
        elif [ "$key" == " " ]; then  # 空格键
            toggle_selection
            render
        elif [ "$key" == "a" ] || [ "$key" == "A" ]; then  # A键
            toggle_all
            render
        elif [ "$key" == "" ] || [ "$key" == $'\n' ] || [ "$key" == $'\r' ]; then  # 回车键
            # 检查是否至少选择了一个
            local has_selection=false
            for ((i=0; i<total; i++)); do
                if [ "${selected[$i]}" == "1" ]; then
                    has_selection=true
                    break
                fi
            done
            
            if [ "$has_selection" == false ]; then
                echo -e "\n${RED}请至少选择一个选项！${RESET}"
                sleep 1
                render
                continue
            fi
            
            # 收集选中的索引，写入全局变量
            __SELECTED_INDICES=()
            for ((i=0; i<total; i++)); do
                if [ "${selected[$i]}" == "1" ]; then
                    __SELECTED_INDICES+=($i)
                fi
            done
            break
        elif [ "$key" == "q" ] || [ "$key" == "Q" ]; then  # Q键退出
            # 恢复终端设置
            stty $old_stty 2>/dev/null
            trap - INT TERM EXIT
            echo -e "\n${DIM}已取消${RESET}"
            exit 0
        fi
    done
    
    # 恢复终端设置
    stty $old_stty 2>/dev/null
    trap - INT TERM EXIT
    
    # 清屏（使用 ANSI 转义码）
    printf "\033[2J\033[H"
}

BASE_DIR=".agents"
# 映射目标路径：把 skills 下的文件写入 $BASE_DIR/skills（剥离前两级）或其他规则
map_target_path() {
    local base="$1"                # base should point to $BASE_DIR
    local relative_path="$2"
    local chosen_dir_name="$3"
    local filename=$(basename "$relative_path")
    # skills/scripts 目录下文件统一写入 $BASE_DIR/skills/scripts/
    if [[ "$relative_path" =~ ^skills/scripts/ ]]; then
        local scripts_inner_path=$(echo "$relative_path" | sed 's|^skills/scripts/||')
        echo "${base}/skills/scripts/${scripts_inner_path}"
        return
    fi
    # 剥离前两级（例如: skills/<chosen>/<rest> -> <rest>）
    local inner_path=$(echo "$relative_path" | sed 's|^[^/]*/[^/]*/||')
    if [[ "$filename" == *.skill.md ]]; then
        echo "${base}/skills/${filename}"
    elif [[ "$filename" == *.sh ]]; then
        echo "${base}/scripts/${filename}"
    else
        if [ -n "$chosen_dir_name" ]; then
            echo "${base}/skills/${chosen_dir_name}/${inner_path}"
        else
            # 没有模板名时，把文件放到 base/skills/<inner_path>（inner_path 对于 skills/<file> 会是原路径）
            # 如果 inner_path 起始包含 "skills/", 去掉它
            local clean_inner=$(echo "$inner_path" | sed 's|^skills/||')
            echo "${base}/skills/${clean_inner}"
        fi
    fi
}

main() {
    check_dependencies

    echo -e "${CYAN}正在从 GitLab 获取 skills/skills.json ...${RESET}"
    local skills_json=$(fetch_file_content "$PROJECT" "$REF" "skills/skills.json")
    local fetch_status=$?
    if [ $fetch_status -ne 0 ] || [ -z "$skills_json" ]; then
        if [ $fetch_status -ne 0 ]; then
            echo -e "${RED}错误: 无法从 GitLab 获取 skills/skills.json${RESET}"
        else
            echo -e "${RED}错误: skills/skills.json 内容为空${RESET}"
        fi
        exit 1
    fi
    echo -e "${GREEN}✓ 成功获取 skills.json${RESET}"

    echo -e "${CYAN}正在解析 skills.json ...${RESET}"

    # 解析 JSON 获取所有选项（保留 description 解析逻辑）
    local skill_names=()
    local skill_descriptions=()
    local skill_ref_strings=()
    while IFS="$FIELD_DELIM" read -r name description refs; do
        if [ -n "$name" ] && [ -n "$description" ]; then
            skill_names+=("$name")
            skill_descriptions+=("$description")
            skill_ref_strings+=("$refs")
        fi
    done < <(parse_skills_json "$skills_json")
    
    if [ ${#skill_names[@]} -eq 0 ]; then
        echo -e "${RED}错误: skills.json 中没有找到有效的技能项${RESET}"
        exit 1
    fi

    echo -e "${GREEN}√ 成功解析 ${#skill_names[@]} 个技能项${RESET}"
    
    # 交互式多选
    interactive_multiselect skill_names skill_descriptions
    
    # 从全局变量获取选中的索引
    local selected_indices=("${__SELECTED_INDICES[@]}")
    
    if [ ${#selected_indices[@]} -eq 0 ]; then
        echo "未选择任何技能，退出。"
        exit 0
    fi
    
    # 获取选中的技能名称
    local selected_names=()
    local selected_refs=()
    for idx in "${selected_indices[@]}"; do
        selected_names+=("${skill_names[$idx]}")
        selected_refs+=("${skill_ref_strings[$idx]}")
    done
    
    echo -e "已选择 ${#selected_names[@]} 个技能: ${selected_names[*]}"
    echo ""
    
    # 根据选中的 name 收集文件
    local all_files=()
    local reference_files=()
    for ((i=0; i<${#selected_names[@]}; i++)); do
        local selected_name="${selected_names[$i]}"
        printf "收集技能文件: %s\n" "$selected_name"
        local skill_dir="skills/${selected_name}"
        while IFS= read -r file; do
            if [ -n "$file" ] && ! array_contains "$file" "${all_files[@]}"; then
                all_files+=("$file")
            fi
        done < <(collect_dir_files "$PROJECT" "$REF" "$skill_dir")

        local refs_string="${selected_refs[$i]}"
        if [ -n "$refs_string" ]; then
            local refs_array=()
            while IFS= read -r ref; do
                if [ -n "$ref" ]; then
                    ref=$(echo "$ref" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
                    if [ -n "$ref" ]; then
                        refs_array+=("$ref")
                    fi
                fi
            done < <(split_references "$refs_string")

            for ref in "${refs_array[@]}"; do
                if [ -z "$ref" ]; then
                    continue
                fi

                local reference_path
                if [[ "$ref" == *.md ]]; then
                    reference_path="skills/references/${ref}"
                else
                    reference_path="skills/references/${ref}.md"
                fi

                if array_contains "$reference_path" "${reference_files[@]}"; then
                    continue
                fi

                reference_files+=("$reference_path")
            done
        fi
    done

    if [ ${#reference_files[@]} -gt 0 ]; then
        printf "额外引用文件: %d 个\n" ${#reference_files[@]}
        for ref_file in "${reference_files[@]}"; do
            if ! array_contains "$ref_file" "${all_files[@]}"; then
                all_files+=("$ref_file")
            fi
        done
    fi

    # 额外全量拉取 skills/scripts
    local extra_dirs=("skills/scripts")
    for extra_dir in "${extra_dirs[@]}"; do
        printf "收集公共目录文件: %s\n" "$extra_dir"
        while IFS= read -r file; do
            if [ -n "$file" ] && ! array_contains "$file" "${all_files[@]}"; then
                all_files+=("$file")
            fi
        done < <(collect_dir_files "$PROJECT" "$REF" "$extra_dir")
    done
    
    if [ ${#all_files[@]} -eq 0 ]; then
        echo "未找到任何文件，退出。"
        exit 0
    fi
    
    printf "共 %d 个文件待写入。\n" ${#all_files[@]}
    echo ""
    
    local base_dir="$(pwd)/$BASE_DIR"
    mkdir -p "$base_dir/skills"
    local success=0
    local idx=0
    for file in "${all_files[@]}"; do
        idx=$((idx + 1))
        # 计算模板名（第二段），如果没有则为空
        local chosen_dir_name=""
        if [[ "$file" =~ ^skills/([^/]+)/ ]]; then
            chosen_dir_name="${BASH_REMATCH[1]}"
        fi
        local target=$(map_target_path "$base_dir" "$file" "$chosen_dir_name")
        local target_dir=$(dirname "$target")
        mkdir -p "$target_dir"
        if fetch_file_content "$PROJECT" "$REF" "$file" > "$target"; then
            printf "${GREEN}√ 写入:${RESET} %s\n" "$target"
            success=$((success + 1))
        else
            printf "${RED}✘ 处理失败: %s${RESET}\n" "$file"
            rm -f "$target"
        fi
    done
    echo ""
    printf "${GREEN}√ Skills 初始化完成 (成功: %d/%d)${RESET}\n" $success ${#all_files[@]}
}

main "$@"
