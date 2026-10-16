"""Write the responsive weather banner family and its content-hashed copies."""

import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


def banner(*, dark, mobile, still):
    width, height = (392, 408) if mobile else (800, 200)
    title, muted = ("#fff", "#a8a29e") if dark else ("#0f172a", "#64748b")
    background = 'fill="url(#background)"' if dark else 'fill="#fff" stroke="#e5e7eb"'
    x, y, scale = (55, 184, 1.5) if mobile else (574, 32, 1)
    title_x, title_y, title_size = (20, 76, 36) if mobile else (32, 80, 40)
    tagline_y, tagline_size = (104, 12) if mobile else (111, 14)
    pills_y, pill_size = (126, 8.5) if mobile else (133, 10)
    pills = (
        [("Live weather", 82), ("No tracking or analytics", 142), ("Moving sky", 84)]
        if mobile
        else [("Live weather", 94), ("No tracking or analytics", 164), ("Moving sky", 94)]
    )
    pill_x = title_x
    pill_markup = []
    for index, (text, length) in enumerate(pills):
        fill = "#1a1a1a" if dark else ("#eff6ff", "#f0fdf4", "#f5f3ff")[index]
        color = ("#60a5fa", "#4ade80", "#a78bfa")[index] if dark else "#1e40af"
        pill_markup.append(
            f'<rect x="{pill_x}" y="{pills_y}" width="{length}" height="22" rx="11" '
            f'fill="{fill}" stroke="{color}" stroke-opacity=".35" stroke-width=".8"/>'
            f'<text x="{pill_x + length / 2}" y="{pills_y + 14.5}" fill="{color}" '
            f'font-size="{pill_size}" font-weight="500" text-anchor="middle">{text}</text>'
        )
        pill_x += length + 8
    style = (
        ""
        if still
        else """
<style>
@keyframes cloudTravel {0%,100%{transform:translateX(-12px)}50%{transform:translateX(12px)}}
@keyframes sunshine {0%,100%{transform:scale(1);opacity:.8}50%{transform:scale(1.07);opacity:1}}
@keyframes windFlow {to{stroke-dashoffset:-24}}
.near,.far,.sun,.wind{transform-box:fill-box;transform-origin:center}
.near{animation:cloudTravel 6s ease-in-out infinite}
.far{animation:cloudTravel 8.25s ease-in-out -2.25s infinite}
.sun{animation:sunshine 3.75s ease-in-out infinite}
.wind{animation:windFlow 3s linear infinite}
@media(prefers-reduced-motion:reduce){.near,.far,.sun,.wind{animation:none}}
</style>"""
    )
    classes = {key: "" if still else f' class="{key}"' for key in ("near", "far", "sun", "wind")}
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}"
viewBox="0 0 {width} {height}" role="img" aria-labelledby="title description">
<title id="title">Skycast Weather</title>
<desc id="description">Live forecasts and a moving sky. No tracking or analytics.</desc>
{style}
<defs>
<linearGradient id="background" x2="1" y2="1">
<stop stop-color="#0c0a09"/><stop offset="1" stop-color="#1c1917"/></linearGradient>
<linearGradient id="sky" x2="0" y2="1">
<stop stop-color="#1e40af"/><stop offset="1" stop-color="#60a5fa"/></linearGradient>
<linearGradient id="cloud" x2="0" y2="1">
<stop stop-color="#fff"/><stop offset="1" stop-color="#cbd5e1"/></linearGradient>
<clipPath id="scene"><rect width="188" height="136" rx="16"/></clipPath>
</defs>
<rect width="{width}" height="{height}" rx="16" {background}/>
<g font-family="system-ui, -apple-system, 'Segoe UI', sans-serif">
<text x="{title_x}" y="{title_y}" font-size="{title_size}"
font-weight="700" letter-spacing="-.5" fill="{title}">Skycast Weather</text>
<text x="{title_x}" y="{tagline_y}" font-size="{tagline_size}" fill="{muted}">
A forecast worth watching.</text>
{"".join(pill_markup)}
</g>
<g transform="translate({x} {y}) scale({scale})"><g clip-path="url(#scene)">
<rect width="188" height="136" fill="url(#sky)"/>
<circle cx="139" cy="36" r="23" fill="#fbbf24"{classes["sun"]}/>
<g transform="translate(96 47) scale(.6)"><g{classes["far"]}>
<path d="M10 45C-5 45-6 20 10 18C14-5 47-6 54 17C77 9 92 44 68 45Z"
fill="#e2e8f0" opacity=".75"/></g></g>
<g transform="translate(27 43)"><g{classes["near"]}>
<path d="M10 49C-7 49-6 23 11 21C13-4 49-9 58 17C83 6 106 48 78 49Z"
fill="url(#cloud)"/></g></g>
<g fill="none" stroke="#dbeafe" stroke-width="2" stroke-linecap="round"
stroke-dasharray="7 5" opacity=".65"{classes["wind"]}>
<path d="M18 108C63 94 116 115 170 102"/><path d="M30 122C74 109 124 130 175 117"/>
</g></g></g>
</svg>
'''


def main():
    art = ROOT / "art"
    art.mkdir(exist_ok=True)
    for old in art.glob("banner-*.*.svg"):
        old.unlink()
    for dark in (False, True):
        for mobile in (False, True):
            for still in (False, True):
                name = "banner-" + ("mobile-" if mobile else "") + ("still-" if still else "")
                name += "dark" if dark else "light"
                body = banner(dark=dark, mobile=mobile, still=still).encode()
                (art / f"{name}.svg").write_bytes(body)
                digest = hashlib.sha256(body).hexdigest()[:12]
                (art / f"{name}.{digest}.svg").write_bytes(body)


if __name__ == "__main__":
    main()
