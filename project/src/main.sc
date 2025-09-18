require: functions.js
require: slotfilling/slotFilling.sc
  module = sys.zb-common
theme: /

    # Начало и приветствие
    state: Start
        q!: $regex</start>
        random:
            a: Здраствуйте! Этот бот служит для нахождения доступных вакансий. Чем могу вам помочь?
            a: Приветсвую, я бот для поиска вакансии. Как я могу вам помочь?
        buttons:
            "Заполнить анкету" -> /Survey
            "Поиск" -> /HandleApiResponse

    state: Hello
        intent!: /привет
        a: Ещё раз здраствуйте)


    # Анкетирование пользователя
    state: Survey
        intent!: /анкета
        random: 
            a: Отлично! Давайте заполним небольшую анкету.
            a: Хорошо, заполним анкету для просмотра вакансий.
            a: Замечательно! Приступим к заполнению анкеты.
        a: Какая профессия вас интересует?

        state: AwaitJob
            q: * @profession *
            a: В каком регионе ищете работу?
            script:
                if (!$session.survey) {
                    $session.survey = {};
                }
                $session.survey.job = $parseTree._profession.name
            
            state: AwaitRegion
                q: * @region *
                a: Какой тип занятости вас интересует? Полная, временная или частичная занятость?
                script:
                    $session.survey.region = $parseTree._region.code;

                state: AwaitEmployment
                    q: * @employment *
                    a: От какого размера заработной платы начинать искать?
                    script:
                        $session.survey.employment = $parseTree._employment.name;
                    
                    state: AwaitSalary
                        q: * @duckling.number *
                        a: Спасибо! Ваша анкета заполнена. Вот что вы указали:
                        script:
                            $session.survey.salary = $entities[0].value;
                        a: Профессия: {{ $session.survey.job }};\n Номер региона: {{ $session.survey.region }};\n Тип занятости: {{ $session.survey.employment }};\n Зарплата: {{ $session.survey.salary }} руб.
                        buttons:
                            "Заполнить анкету ещё раз" -> /Survey
                            "Поиск вакансий" -> /HandleApiResponse
                            

    # Поиск вакансий
    state: HandleApiResponse
        intent!: /поиск
        script:
            if (!$session.survey) {
                $reactions.answer("К сожалению нет ваших данных :( Необходимо заполнить анкету.");
                $reactions.buttons({ text: "Заполнить анкету", transition: "/Survey" });
            }
            else {
                # Выполняем запрос через API для поиска вакансий
                fetchVacancies($session.survey).then(function (res) {
                    if (res.status === '200' && res.results.vacancies.length > 0) {
                        $session.vacancies = res.results.vacancies;
                        $session.page = 0;
                        $reactions.answer("Вакансии успешно найдены! Перехожу к отображению...");
                        showPage($session.page, $session.vacancies);
                    } else {
                        $reactions.answer("К сожалению, вакансии по вашему запросу не найдены.");
                    }
                }).catch(function (err) {
                    $reactions.answer("Что-то сервер барахлит. Не ничего найти.");
                });
            }
    
        state: NextPage
            q: *след*
            script:
                if ($session.page !== undefined && $session.vacancies) {
                    $session.page++;
                    if ($session.page * 3 < $session.vacancies.length) {
                        showPage($session.page, $session.vacancies);
                    } else {
                        $reactions.answer("Это последняя страница.");
                    }
                } else {
                    $reactions.answer("Сначала выполните поиск вакансий.");
                }
                
        state: PrevPage
            q: *пред*
            script:
                if ($session.page > 0) {
                    $session.page--;
                    showPage($session.page, $session.vacancies);
                } else {
                    $reactions.answer("Это первая страница.");
                }
               
               
    # Рекомендация вакансий
    state: Recommendations
        intent!: /рекомендация
        random:
            a: Запросто! Сейчас что-нибудь придумаю.
            a: Хорошо, постараюсь подобрать.
        script:
            if (!$session.rec) {
                    $session.rec = {};
            }
            $session.rec.sphere = $parseTree._sphere.name;
            $session.rec.area = $parseTree._area.name;
            fetchRecommendations($session.rec.sphere, $session.rec.area).then(function (res) {
                if (res.status_code === 200 && res.recommendations.length > 0) {
                    $session.recommendations = res.recommendations;
                    $session.page = 0;
                    $reactions.answer("Постарался подобрать. Посмотрите...");
                    showPage($session.page, $session.recommendations);
                } else {
                    $reactions.answer("К сожалению, не могу решить что вам порекомендовать :(");
                }
            }).catch(function (err) {
                $reactions.answer("Что-то сервер барахлит. Не могу получить рекомендации.");
            });
                
    
        state: NextPage
            q: *след*
            script:
                if ($session.page !== undefined && $session.recommendations) {
                    $session.page++;
                    if ($session.page * 3 < $session.recommendations.length) {
                        showPage($session.page, $session.recommendations);
                    } else {
                        $reactions.answer("Это последняя страница.");
                    }
                } else {
                    $reactions.answer("Сначала выполните поиск вакансий.");
                }
                
        state: PrevPage
            q: *пред*
            script:
                if ($session.page > 0) {
                    $session.page--;
                    showPage($session.page, $session.recommendations);
                } else {
                    $reactions.answer("Это первая страница.");
                }
               
                
                
    state: Bye
        intent!: /пока
        random:
            a: Всего доброго! Рад был помочь)
            a: До свидания! Надеюсь смог вам помочь
            a: Удачи! Рад что был вам полезен)
        script:
            $jsapi.stopSession();



    state: NoMatch
        event!: noMatch
        random:
            a: Извините, я не понимаю. Переформулируйте, пожалуйста
            a: Простите, кажется я вас не понял. Напишите своё предложение иначе.
            a: К сожалению я не понял, что значит следующее сообщение: {{$request.query}}
        buttons:
            "Заполнить анкету" -> /Survey
            "Поиск" -> /HandleApiResponse



    state: Profanity
        q!: * @mystem.obsc *
        random:
            a: Ваше сообщение содержит нецензурные слова. Пожалуйста, используйте более корректные выражения.
            a: Пожалуйста, избегайте нецензурных выражений. Мы стараемся поддерживать уважительную атмосферу.
            a: Давайте соблюдать нормы приличия и не использовать подобные выражения.
        buttons:
            "Заполнить анкету" -> /Survey
            "Поиск" -> /HandleApiResponse

    state: ProfanityAlt
        q!: * @mlps-obscene.obscene *
        random:
            a: Ваше сообщение содержит нецензурные слова. Пожалуйста, используйте более корректные выражения.
            a: Пожалуйста, избегайте нецензурных выражений. Мы стараемся поддерживать уважительную атмосферу.
            a: Давайте соблюдать нормы приличия и не использовать подобные выражения.
        buttons:
            "Заполнить анкету" -> /Survey
            "Поиск" -> /HandleApiResponse