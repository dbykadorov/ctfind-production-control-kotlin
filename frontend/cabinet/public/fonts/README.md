# Cabinet fonts (DEPRECATED)

Эта папка больше не используется. Шрифты теперь приходят из npm-пакетов:

* `@fontsource-variable/inter` (`Inter Variable`)
* `@fontsource-variable/jetbrains-mono` (`JetBrains Mono Variable`)

Импорт — в `src/styles/fonts.css`. Vite сам бандлит woff2 в финальный билд
(`public/cabinet_app/assets/<hash>.woff2`) и обновляет URL'ы в CSS.

Папку `public/fonts/` можно удалить вместе с этим README, когда у вас будет
уверенность, что нигде в инфраструктуре (nginx, скрипты деплоя, fixtures)
на неё нет ссылок.
