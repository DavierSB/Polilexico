import { svg } from "./dom.js";

const PATHS = {
  back: "M20 11H7.8l5.6-5.6L12 4l-8 8 8 8 1.4-1.4L7.8 13H20z",
  pause: "M6 5h4v14H6zM14 5h4v14h-4z",
  play: "M8 5v14l11-7z",
  palette: "M12 3a9 9 0 0 0 0 18c.8 0 1.5-.7 1.5-1.5 0-.4-.1-.7-.4-1-.2-.3-.4-.6-.4-1 0-.8.7-1.5 1.5-1.5H16a5 5 0 0 0 5-5c0-4.4-4-8-9-8zm-5.5 9a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3zm3-4a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3zm5 0a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3zm3 4a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3z",
  chat: "M4 4h16a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H8l-4 4V6a2 2 0 0 1 2-2zm2 5v2h12V9zm0 4v2h8v-2z",
  send: "M3 20.5V14l9-2-9-2V3.5L22 12z",
  person: "M12 12a4.5 4.5 0 1 0 0-9 4.5 4.5 0 0 0 0 9zm0 2c-4 0-8 2-8 5v2h16v-2c0-3-4-5-8-5z",
  eye: "M12 5C6.5 5 2.7 8.6 1 12c1.7 3.4 5.5 7 11 7s9.3-3.6 11-7c-1.7-3.4-5.5-7-11-7zm0 11.5a4.5 4.5 0 1 1 0-9 4.5 4.5 0 0 1 0 9zm0-2.5a2 2 0 1 0 0-4 2 2 0 0 0 0 4z",
  close: "M18.3 5.7 16.9 4.3 12 9.2 7.1 4.3 5.7 5.7 10.6 10.6 5.7 15.5l1.4 1.4 4.9-4.9 4.9 4.9 1.4-1.4-4.9-4.9z",
  globe: "M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm6.9 6h-3a15.7 15.7 0 0 0-1.3-3.6A8 8 0 0 1 18.9 8zM12 4c.8 1.2 1.5 2.5 1.9 4h-3.8c.4-1.5 1.1-2.8 1.9-4zM4.3 14a8 8 0 0 1 0-4h3.4a16.5 16.5 0 0 0 0 4zm.8 2h3a15.7 15.7 0 0 0 1.3 3.6A8 8 0 0 1 5.1 16zm3-8h-3a8 8 0 0 1 4.3-3.6C8.8 5.5 8.4 6.7 8.1 8zM12 20c-.8-1.2-1.5-2.5-1.9-4h3.8c-.4 1.5-1.1 2.8-1.9 4zm2.3-6H9.7a14.7 14.7 0 0 1 0-4h4.6a14.7 14.7 0 0 1 0 4zm.3 5.6c.6-1.1 1-2.3 1.3-3.6h3a8 8 0 0 1-4.3 3.6zm1.7-5.6a16.5 16.5 0 0 0 0-4h3.4a8 8 0 0 1 0 4z",
};

export function icon(name, size = 24) {
  return svg(`<svg viewBox="0 0 24 24" width="${size}" height="${size}" aria-hidden="true"><path fill="currentColor" d="${PATHS[name]}"/></svg>`);
}

export function bagIcon(count) {
  return svg(`<svg viewBox="0 0 48 56" width="40" height="46" aria-hidden="true">
    <path d="M17 6c2 4 12 4 14 0l-3 9h-8z" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linejoin="round"/>
    <path d="M19 15h10c9 6 15 15 15 24 0 9-8 14-20 14S4 48 4 39c0-9 6-18 15-24z" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linejoin="round"/>
    <text x="24" y="42" text-anchor="middle" font-size="15" font-weight="700" fill="currentColor" font-family="Roboto, Arial, sans-serif">${count}</text>
  </svg>`);
}
