import requests
from bs4 import BeautifulSoup
import json
import os
import time

def scrape_cambridge_ipa_basic():
    url = "https://dictionary.cambridge.org/help/phonetics.html"
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"
    }

    script_dir = os.path.dirname(os.path.abspath(__file__))
    audio_dir = os.path.join(script_dir, "ipa_basic_sounds")

    if not os.path.exists(audio_dir):
        os.makedirs(audio_dir)

    print(f"Đang kiểm tra dữ liệu từ {url}...")
    try:
        response = requests.get(url, headers=headers, timeout=30)
    except Exception as e:
        print(f"Lỗi kết nối trang chủ: {e}")
        return

    soup = BeautifulSoup(response.content, 'html.parser')
    rows = soup.find_all('tr')

    targets = []
    for row in rows:
        ipa_td = row.find('td', class_='ipa-sound')
        if not ipa_td: continue

        ipa_symbol = ipa_td.get_text(strip=True)
        voice_td = ipa_td.find_next_sibling('td', class_='hvm')

        if voice_td:
            audio_elements = voice_td.find_all('audio')
            regions = voice_td.find_all('span', class_='audio-region')
            for i, audio in enumerate(audio_elements):
                region = regions[i].get_text(strip=True) if i < len(regions) else "Unknown"
                source = audio.find('source', type='audio/mpeg')
                if source and source.get('src'):
                    audio_url = "https://dictionary.cambridge.org" + source.get('src')
                    safe_ipa = ipa_symbol.replace("/", "").replace(":", "ː").replace("*", "").replace("?", "").strip()
                    file_name = f"{region.lower()}_{safe_ipa}.mp3"
                    targets.append({
                        "ipa": ipa_symbol, "region": region, "url": audio_url,
                        "path": os.path.join(audio_dir, file_name), "name": file_name
                    })

    print(f"Tổng số âm IPA: {len(targets)}")
    data_list = []
    download_count = 0

    for idx, item in enumerate(targets):
        # CHỈ TẢI NẾU FILE CHƯA TỒN TẠI
        if not os.path.exists(item["path"]):
            success = False
            for retry in range(3): # Thử lại tối đa 3 lần
                try:
                    print(f"({idx+1}/{len(targets)}) Đang tải: {item['name']} (Lần thử {retry+1})")
                    res = requests.get(item["url"], headers=headers, timeout=30)
                    if res.status_code == 200:
                        with open(item["path"], 'wb') as f:
                            f.write(res.content)
                        success = True
                        download_count += 1
                        break
                except Exception:
                    time.sleep(1)
            if not success:
                print(f"❌ Thất bại sau 3 lần thử: {item['name']}")

        data_list.append({
            "ipa": item["ipa"], "region": item["region"],
            "url": item["url"], "local_file": item["name"]
        })

    # Lưu JSON
    output_json = os.path.join(script_dir, "ipa_basic_voices.json")
    with open(output_json, 'w', encoding='utf-8') as f:
        json.dump(data_list, f, ensure_ascii=False, indent=4)

    print(f"\n✅ HOÀN THÀNH!")
    print(f"- Đã tải mới: {download_count} files.")
    print(f"- Tổng số file trong thư mục: {len(os.listdir(audio_dir))}")
    print(f"- File JSON: {output_json}")

if __name__ == "__main__":
    scrape_cambridge_ipa_basic()