#!/usr/bin/env node
import { execFileSync } from 'node:child_process';

const adb = process.env.ADB || '/home/ekrupin/bin/wadb';
const pkg = 'ru.ekrupin.ivi';
const devices = process.argv.slice(2);

if (devices.length !== 2) {
  console.error('Usage: ADB=/path/to/adb scripts/android-two-device-smoke.mjs <device1> <device2>');
  process.exit(2);
}

const ids = {
  appRoot: 'ivi_app_root',
  navSettings: 'nav_settings',
  settingsRoot: 'settings_root',
  syncSection: 'settings_sync_section',
  baseUrl: 'settings_sync_base_url_field',
  email: 'settings_sync_email_field',
  displayName: 'settings_sync_display_name_field',
  password: 'settings_sync_password_field',
  login: 'settings_login_button',
  register: 'settings_register_button',
  manualSync: 'settings_manual_sync_button',
  connectionStatus: 'settings_connection_status',
};

function adbExec(device, args, options = {}) {
  return execFileSync(adb, ['-s', device, ...args], {
    encoding: options.encoding ?? 'utf8',
    stdio: options.stdio ?? ['ignore', 'pipe', 'pipe'],
  });
}

function wait(device, ms = 700) {
  adbExec(device, ['shell', 'sleep', String(ms / 1000)]);
}

function dumpXml(device) {
  return adbExec(device, ['exec-out', 'uiautomator', 'dump', '/dev/tty']);
}

function nodeByResourceId(xml, resourceId) {
  const escaped = resourceId.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const match = xml.match(new RegExp(`<node[^>]*resource-id="${escaped}"[^>]*bounds="\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]"[^>]*/?>`));
  if (!match) return null;
  const [, x1, y1, x2, y2] = match.map(Number);
  return { x: Math.round((x1 + x2) / 2), y: Math.round((y1 + y2) / 2) };
}

function requireId(device, resourceId) {
  const xml = dumpXml(device);
  const node = nodeByResourceId(xml, resourceId);
  if (!node) {
    throw new Error(`${device}: resource-id ${resourceId} not found`);
  }
  return node;
}

function requireIdWithScroll(device, resourceId, maxScrolls = 6) {
  for (let attempt = 0; attempt <= maxScrolls; attempt += 1) {
    const xml = dumpXml(device);
    const node = nodeByResourceId(xml, resourceId);
    if (node) return node;
    adbExec(device, ['shell', 'input', 'swipe', '540', '1900', '540', '700', '500']);
    wait(device, 500);
  }
  throw new Error(`${device}: resource-id ${resourceId} not found after scrolling`);
}

function tapId(device, resourceId) {
  const { x, y } = requireId(device, resourceId);
  adbExec(device, ['shell', 'input', 'tap', String(x), String(y)]);
}

function launch(device) {
  adbExec(device, ['reverse', 'tcp:8080', 'tcp:8080']);
  adbExec(device, ['shell', 'am', 'start', '-n', `${pkg}/.MainActivity`], { stdio: ['ignore', 'ignore', 'pipe'] });
  wait(device, 1200);
}

function checkBaseSelectors(device) {
  requireId(device, ids.appRoot);
  requireId(device, ids.navSettings);
  tapId(device, ids.navSettings);
  wait(device);
  requireId(device, ids.settingsRoot);
  requireIdWithScroll(device, ids.syncSection);
  requireIdWithScroll(device, ids.baseUrl);
  requireIdWithScroll(device, ids.connectionStatus);
  requireIdWithScroll(device, ids.manualSync);
}

function checkAuthSelectorsWhenLoggedOut(device) {
  const xml = dumpXml(device);
  for (const resourceId of [ids.email, ids.displayName, ids.password, ids.login, ids.register]) {
    if (!nodeByResourceId(xml, resourceId)) {
      console.warn(`${device}: ${resourceId} is not visible; device may already be logged in`);
    }
  }
}

for (const device of devices) {
  launch(device);
  checkBaseSelectors(device);
  checkAuthSelectorsWhenLoggedOut(device);
  console.log(`${device}: OK`);
}

console.log('Two-device UI selector smoke passed');
