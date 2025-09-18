import requests
import random
from flask import Flask, jsonify, request
from experta import *

app = Flask(__name__)

class ProfessionalRecommendationSystem(KnowledgeEngine):
    def __init__(self):
        super().__init__()
        self.result = []

    # IT-направления
    @Rule(Fact(sphere='IT'), Fact(area='Аналитика'))
    def it_web_recommendation(self):
        self.result.append("Анализ данных python")
        self.result.append("Scikit-Learn")

    @Rule(Fact(sphere='IT'), Fact(area='Создание сайтов'))
    def it_security_recommendation(self):
        self.result.append("Vue")
        self.result.append("Wordpress")

    @Rule(Fact(sphere='IT'), Fact(area='Машинное обучение'))
    def it_ml_recommendation(self):
        self.result.append("Data Scientist")
        self.result.append("ML Инженер")

    # Медицинские направления
    @Rule(Fact(sphere='Медицина'), Fact(area='Стоматология'))
    def medicine_dentistry_recommendation(self):
        self.result.append("Стоматолог-терапевт")
        self.result.append("Стоматолог-ортопед")

    @Rule(Fact(sphere='Медицина'), Fact(area='Хирургия'))
    def medicine_surgery_recommendation(self):
        self.result.append("Хирург общей практики")
        self.result.append("Нейрохирург")

    @Rule(Fact(sphere='Медицина'), Fact(area='Педиатрия'))
    def medicine_pediatrics_recommendation(self):
        self.result.append("Педиатр")
        self.result.append("Детский невролог")
        

@app.route('/vacancy_recommendation', methods=['GET'])
def recommend_vacancy():
    sphere = request.args.get('sphere')  # Получаем сферу деятельности
    area = request.args.get('area')  # Получаем область этой сферы
    
    if not sphere or not area:
        return jsonify(message="Необходимы параметры: сфера и область."), 400
    
    # Создаем экземпляр экспертной системы
    kb = ProfessionalRecommendationSystem()
    
    # Сбрасываем состояние экспертной системы
    kb.reset()
    
    # Декларируем факты
    kb.declare(Fact(sphere=sphere))
    kb.declare(Fact(area=area))

    # Запускаем экспертную систему
    kb.run()
    
    # Собираем вакансии для каждой рекомендации
    final_recommendations = []
    
    if kb.result:
        for recommendation in kb.result:
            try:
                # Запрос к API для получения вакансий
                response = requests.get(
                    "http://opendata.trudvsem.ru/api/v1/vacancies", 
                    params={"text": recommendation, "limit": 10}
                )
                
                if response.status_code == 200:
                    data = response.json()  # Получаем JSON-данные
                    vacancies = data['results']['vacancies']
                    
                    # Выбираем 3 случайные вакансии, если есть
                    if vacancies:
                        selected_vacancies = random.sample(vacancies, min(3, len(vacancies)))
                        final_recommendations.extend(selected_vacancies)
                else:
                    print(f"Ошибка при получении данных: {response.status_code} - {response.text}")
                        
            except requests.RequestException as e:
                print(f"Ошибка при запросе вакансий для {recommendation}: {e}")
            
        return jsonify(
            status_code=200,
            sphere=sphere, 
            area=area, 
            recommendations=final_recommendations
        )
        
    else:
        no_recommendations = ["Нет доступных рекомендаций."]
        return jsonify(status_code=500, sphere=sphere, area=area, recommendations=no_recommendations)

if __name__ == '__main__':
    app.run(debug=True)