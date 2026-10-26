import os
from PIL import Image, ImageDraw, ImageFont

os.makedirs("store_assets", exist_ok=True)
os.makedirs("C:/Users/jeric/Desktop/DrinkStock_Store_Assets", exist_ok=True)

def create_screenshot(filename, title, subtitle, badge, main_color, card_title, card_sub, item1, item2, item3):
    img = Image.new("RGBA", (1080, 1920), (18, 19, 28, 255))
    draw = ImageDraw.Draw(img)

    # Top Header Banner
    draw.rectangle([0, 0, 1080, 420], fill=main_color)

    try:
        font_title = ImageFont.truetype("arial.ttf", 52)
        font_sub = ImageFont.truetype("arial.ttf", 28)
        font_card = ImageFont.truetype("arial.ttf", 36)
        font_text = ImageFont.truetype("arial.ttf", 26)
    except:
        font_title = ImageFont.load_default()
        font_sub = ImageFont.load_default()
        font_card = ImageFont.load_default()
        font_text = ImageFont.load_default()

    # Title & Subtitle in Top Banner
    draw.text((60, 100), title, fill=(255, 255, 255, 255), font=font_title)
    draw.text((60, 180), subtitle, fill=(240, 240, 240, 255), font=font_sub)

    # Phone Frame Container
    draw.rounded_rectangle([80, 360, 1000, 1820], radius=40, fill=(28, 30, 42, 255), outline=(60, 60, 80, 255), width=4)

    # App Bar Inside Phone
    draw.rounded_rectangle([110, 400, 970, 520], radius=20, fill=(229, 9, 20, 255))
    draw.text((140, 440), "DrinkStock Manager", fill=(255, 255, 255, 255), font=font_card)

    # Badge Pill
    draw.rounded_rectangle([110, 550, 420, 600], radius=12, fill=main_color)
    draw.text((130, 560), badge, fill=(255, 255, 255, 255), font=font_text)

    # Main Card
    draw.rounded_rectangle([110, 630, 970, 980], radius=24, fill=(40, 42, 58, 255))
    draw.text((140, 660), card_title, fill=(255, 255, 255, 255), font=font_card)
    draw.text((140, 720), card_sub, fill=(0, 229, 255, 255), font=font_sub)

    # Card Content Items
    draw.rounded_rectangle([140, 800, 940, 940], radius=16, fill=(50, 52, 70, 255))
    draw.text((170, 840), item1, fill=(255, 255, 255, 255), font=font_text)

    # Item Card 2
    draw.rounded_rectangle([110, 1010, 970, 1360], radius=24, fill=(40, 42, 58, 255))
    draw.text((140, 1040), "Store Softdrink Inventory Lines", fill=(255, 255, 255, 255), font=font_card)
    draw.rounded_rectangle([140, 1120, 940, 1320], radius=16, fill=(50, 52, 70, 255))
    draw.text((170, 1160), item2, fill=(255, 255, 255, 255), font=font_text)

    # Item Card 3
    draw.rounded_rectangle([110, 1390, 970, 1740], radius=24, fill=(40, 42, 58, 255))
    draw.text((140, 1420), "Quick POS Actions & Security", fill=(255, 255, 255, 255), font=font_card)
    draw.rounded_rectangle([140, 1500, 940, 1700], radius=16, fill=(50, 52, 70, 255))
    draw.text((170, 1540), item3, fill=(255, 255, 255, 255), font=font_text)

    path1 = f"store_assets/{filename}"
    path2 = f"C:/Users/jeric/Desktop/DrinkStock_Store_Assets/{filename}"
    img.save(path1)
    img.save(path2)

# Generate 4 Screenshots
create_screenshot("screenshot_1_home.png", "Smart Softdrinks Inventory", "Track stock levels, cost & selling prices in real time", "SMART INVENTORY", (229, 9, 20, 255), "Total Stock: 284 Units", "Total Inventory Value: P 14,200.00", "🥤 Coca-Cola 1.5L - Stock: 48 (P 65.00)", "🥤 Sprite 1.5L - Stock: 36 (P 65.00)", "⚡ Quick Actions: +12 Restock & Record Sale")
create_screenshot("screenshot_2_sales.png", "Encrypted Sales & Profit Reports", "Watch rewarded video ads to decrypt full sales logs", "ADMOB REVENUE", (255, 42, 109, 255), "🔒 Sales & Revenue Data Encrypted", "Watch Video Ad to Decrypt & View Figures", "📊 Today, Yesterday, This Week, This Month Reports", "💰 Net Profit & Gross Revenue Calculation", "🎬 AdMob Rewarded Video Ad Integration")
create_screenshot("screenshot_3_tutorial.png", "Interactive App Tutorial", "Learn how to manage softdrinks, sales & PIN security", "WALKTHROUGH", (0, 229, 255, 255), "Step 1: Check Inventory & Restock", "Step 2: Record Customer POS Sales", "Step 3: View Reports & Earn AdMob Rewards", "Step 4: Store Admin PIN Protection", "🚀 Fast & Easy Onboarding Guide")
create_screenshot("screenshot_4_profile.png", "Admin PIN Security & Cloud Sync", "Protect store edits and synchronize data across devices", "ADMIN PROTECTION", (46, 125, 50, 255), "Admin Security: Active • PIN Protected", "Customizable Admin Security PIN", "🔐 Change Admin Security PIN Anytime", "☁️ Google Firebase Cloud Data Synchronization", "📱 Quick Demo / Guest Mode Session")

print("4 Phone screenshots generated successfully!")
